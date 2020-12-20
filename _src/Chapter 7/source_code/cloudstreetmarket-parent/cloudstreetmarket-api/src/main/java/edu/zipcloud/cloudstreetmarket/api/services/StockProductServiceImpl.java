package edu.zipcloud.cloudstreetmarket.api.services;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.NoResultException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.yahoo.api.Yahoo2;
import org.springframework.social.yahoo.module.ChartHistoMovingAverage;
import org.springframework.social.yahoo.module.ChartHistoSize;
import org.springframework.social.yahoo.module.ChartHistoTimeSpan;
import org.springframework.social.yahoo.module.ChartType;
import org.springframework.social.yahoo.module.YahooQuote;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import edu.zipcloud.cloudstreetmarket.core.converters.YahooQuoteToStockQuoteConverter;
import edu.zipcloud.cloudstreetmarket.core.daos.ChartStockRepository;
import edu.zipcloud.cloudstreetmarket.core.daos.IndexRepository;
import edu.zipcloud.cloudstreetmarket.core.daos.MarketRepository;
import edu.zipcloud.cloudstreetmarket.core.daos.StockProductRepository;
import edu.zipcloud.cloudstreetmarket.core.daos.StockQuoteRepository;
import edu.zipcloud.cloudstreetmarket.core.entities.ChartStock;
import edu.zipcloud.cloudstreetmarket.core.entities.Index;
import edu.zipcloud.cloudstreetmarket.core.entities.Market;
import edu.zipcloud.cloudstreetmarket.core.entities.StockProduct;
import edu.zipcloud.cloudstreetmarket.core.entities.StockQuote;
import edu.zipcloud.cloudstreetmarket.core.enums.MarketId;
import edu.zipcloud.cloudstreetmarket.core.enums.Role;
import edu.zipcloud.cloudstreetmarket.core.services.ExchangeService;
import edu.zipcloud.cloudstreetmarket.core.services.SocialUserService;
import edu.zipcloud.cloudstreetmarket.core.specifications.ChartSpecifications;
import edu.zipcloud.cloudstreetmarket.core.specifications.ProductSpecifications;
import edu.zipcloud.cloudstreetmarket.core.util.AuthenticationUtil;
import edu.zipcloud.core.util.DateUtil;

@Service
@Transactional(readOnly = true)
public class StockProductServiceImpl implements StockProductService {
	
	@Autowired
	private StockProductRepository stockProductRepository;
	
	@Autowired
	private StockQuoteRepository stockQuoteRepository;
	
	@Autowired
	private IndexRepository indexRepository;

	@Autowired
	private MarketRepository marketRepository;
	
	@Autowired
	private SocialUserService usersConnectionRepository;
	
	@Autowired
	private ConnectionRepository connectionRepository;
	
	@Autowired
	private YahooQuoteToStockQuoteConverter yahooStockQuoteConverter;

	@Autowired
	private ChartStockRepository chartStockRepository;
	
	@Autowired
	private ExchangeService exchangeService;
	
    @Autowired
	public Environment env;
	
	@Override
	public Page<StockProduct> get(String indexId, String exchangeId, MarketId marketId, String startWith, Specification<StockProduct> spec, Pageable pageable, boolean validResults) {
		if(!StringUtils.isEmpty(indexId)){
			Index index = indexRepository.findOne(indexId);
			if(index == null){
				throw new NoResultException("No result found for the index ID "+indexId+" !");
			}
			return stockProductRepository.findByIndices(index, pageable);
		}

		if(!StringUtils.isEmpty(startWith)){
			spec = Specifications.where(spec).and(new ProductSpecifications<StockProduct>().nameStartsWith(startWith));
		}
		
		if(marketId != null){
			Market market = marketRepository.findOne(marketId);
			if(market == null){
				throw new NoResultException("No result found for the market ID "+marketId+" !");
			}
			spec = Specifications.where(spec).and(new ProductSpecifications<StockProduct>().marketIdEquals(marketId));
		}
		
		if(!StringUtils.isEmpty(exchangeId)){
			spec = Specifications.where(spec).and(new ProductSpecifications<StockProduct>().exchangeIdEquals(exchangeId));
		}
		
		spec = Specifications.where(spec)
				.and(new ProductSpecifications<StockProduct>().nameNotNull())
				.and(new ProductSpecifications<StockProduct>().exchangeNotNull());

		if(validResults){
			spec = Specifications.where(spec)
					.and(new ProductSpecifications<StockProduct>().currencyNotNull())
					.and(new ProductSpecifications<StockProduct>().hasAPrice());
		}
		
		return stockProductRepository.findAll(spec, pageable);
	}

	@Override
	public StockProduct get(String stockProductId) {
		return stockProductRepository.findOne(stockProductId);
	}

	@Override
	@Transactional
	public Page<StockProduct> gather(String indexId, String exchangeId,
			MarketId marketId, String startWith,
			Specification<StockProduct> spec, Pageable pageable) {

		Page<StockProduct> stocks = get(indexId, exchangeId, marketId, startWith, spec, pageable, false);
		
		if(AuthenticationUtil.userHasRole(Role.ROLE_OAUTH2)){
			updateStocksAndQuotesFromYahoo(stocks.getContent().stream().collect(Collectors.toSet()));
			return get(indexId, exchangeId, marketId, startWith, spec, pageable, false);
		}
		
		return stocks;
	}

	private void updateStocksAndQuotesFromYahoo(Set<StockProduct> askedContent) {
		if(askedContent.isEmpty()){
			return;
		}
		
		Set<StockProduct> recentlyUpdated = askedContent.stream()
			.filter(t -> t.getLastUpdate() != null && DateUtil.isRecent(t.getLastUpdate(), 1))
			.collect(Collectors.toSet());

		if(askedContent.size() != recentlyUpdated.size()){
			
			String guid = AuthenticationUtil.getPrincipal().getUsername();
			String token = usersConnectionRepository.getRegisteredSocialUser(guid).getAccessToken();
			Connection<Yahoo2> connection = connectionRepository.getPrimaryConnection(Yahoo2.class);
			
	        if (connection != null) {
				askedContent.removeAll(recentlyUpdated);

				Map<String, StockProduct> updatableTickers = askedContent.stream()
						.collect(Collectors.toMap(StockProduct::getId, Function.identity()));
				
				List<YahooQuote> yahooQuotes = connection.getApi().financialOperations().getYahooQuotes(new ArrayList<String>(updatableTickers.keySet()), token);
				
				Set<StockProduct> updatableProducts = yahooQuotes.stream()
					.filter(yq -> StringUtils.isNotBlank(yq.getExchange()))
					.map(yq -> {
						StockQuote sq = new StockQuote(yq, updatableTickers.get((yq.getId())));
						return syncProduct(updatableTickers.get((yq.getId())), sq);
					})
					.collect(Collectors.toSet());
				
				if(!updatableProducts.isEmpty()){
					stockProductRepository.save(updatableProducts);
				}

				
				//This job below should decrease with the time
				Set<StockProduct> removableProducts = yahooQuotes.stream()
						.filter(yq -> StringUtils.isBlank(yq.getExchange()))
						.map(yq -> {
							StockQuote sq = new StockQuote(yq, updatableTickers.get((yq.getId())));
							return syncProduct(updatableTickers.get((yq.getId())), sq);
						})
						.collect(Collectors.toSet());
					
				if(!removableProducts.isEmpty()){
					stockProductRepository.delete(removableProducts);
				}
	        }
		}
	}

	@Override
	@Transactional
	public StockProduct gather(String stockProductId) {
		StockProduct stock = stockProductRepository.findOne(stockProductId);
		if(AuthenticationUtil.userHasRole(Role.ROLE_OAUTH2)){
			updateStocksAndQuotesFromYahoo(stock != null ? Sets.newHashSet(stock) : Sets.newHashSet(new StockProduct(stockProductId)));
			return stockProductRepository.findOne(stockProductId);
		}
		return stock;
	}
	
	@Override
	@Transactional
	public List<StockProduct> gather(String[] stockProductId) {
		List<StockProduct> stockProducts = stockProductRepository.findByIdIn(Arrays.asList(stockProductId));
		if(AuthenticationUtil.userHasRole(Role.ROLE_OAUTH2)){
			Set<StockProduct> fallBack = Arrays.asList(stockProductId).stream().map(id -> new StockProduct(id)).collect(Collectors.toSet());
			updateStocksAndQuotesFromYahoo(!stockProducts.isEmpty()? Sets.newHashSet(stockProducts) : fallBack);
			return stockProductRepository.findByIdIn(Arrays.asList(stockProductId));
		}
		return stockProducts;
	}

	@Override
	@Transactional
	public ChartStock gather(String stockProductId, ChartType type,
			ChartHistoSize histoSize, ChartHistoMovingAverage histoAverage,
			ChartHistoTimeSpan histoPeriod, Integer intradayWidth,
			Integer intradayHeight) throws ResourceNotFoundException {
		Preconditions.checkNotNull(type, "ChartType must not be null!");
		
		StockProduct stock = gather(stockProductId);

		ChartStock chartStock = getChartStock(stock, type, histoSize, histoAverage, histoPeriod, intradayWidth, intradayHeight);

		if(AuthenticationUtil.userHasRole(Role.ROLE_OAUTH2)){
			updateChartStockFromYahoo(stock, type, histoSize, histoAverage, histoPeriod, intradayWidth, intradayHeight);
			return getChartStock(stock, type, histoSize, histoAverage, histoPeriod, intradayWidth, intradayHeight);
		}
		else if(chartStock != null){
			return chartStock;
		}
		throw new ResourceNotFoundException();
	}

	private void updateChartStockFromYahoo(StockProduct stock, ChartType type, ChartHistoSize histoSize, ChartHistoMovingAverage histoAverage,
			ChartHistoTimeSpan histoPeriod, Integer intradayWidth, Integer intradayHeight) {
		
		Preconditions.checkNotNull(stock, "stock must not be null!");
		Preconditions.checkNotNull(type, "ChartType must not be null!");
		
		String guid = AuthenticationUtil.getPrincipal().getUsername();
		String token = usersConnectionRepository.getRegisteredSocialUser(guid).getAccessToken();
		Connection<Yahoo2> connection = connectionRepository.getPrimaryConnection(Yahoo2.class);
		
        if (connection != null) {
			byte[] yahooChart = connection.getApi().financialOperations().getYahooChart(stock.getId(), type, histoSize, histoAverage, histoPeriod, intradayWidth, intradayHeight, token);
			
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm");
			LocalDateTime dateTime = LocalDateTime.now();
			String formattedDateTime = dateTime.format(formatter); // "1986-04-08_1230"
			String imageName = stock.getId().toLowerCase()+"_"+type.name().toLowerCase()+"_"+formattedDateTime+".png";
	    	String pathToYahooPicture = env.getProperty("pictures.yahoo.path").concat("\\").concat(imageName);
	    	
            try {
                Path newPath = Paths.get(pathToYahooPicture);
                Files.write(newPath, yahooChart, StandardOpenOption.CREATE);
            } catch (IOException e) {
                throw new Error("Storage of " + pathToYahooPicture+ " failed", e);
            }
            
            ChartStock chartStock = new ChartStock(stock, type, histoSize, histoAverage, histoPeriod, intradayWidth, intradayHeight, pathToYahooPicture);
            chartStockRepository.save(chartStock);
        }
	}
	
	@Override
	public ChartStock getChartStock(StockProduct index, ChartType type,
			ChartHistoSize histoSize, ChartHistoMovingAverage histoAverage,
			ChartHistoTimeSpan histoPeriod, Integer intradayWidth,
			Integer intradayHeight) {
		
		Specification<ChartStock> spec = new ChartSpecifications<ChartStock>().typeEquals(type);
		
		if(type.equals(ChartType.HISTO)){
			if(histoSize != null){
				spec = Specifications.where(spec).and(new ChartSpecifications<ChartStock>().sizeEquals(histoSize));
			}
			if(histoAverage != null){
				spec = Specifications.where(spec).and(new ChartSpecifications<ChartStock>().histoMovingAverageEquals(histoAverage));
			}
			if(histoPeriod != null){
				spec = Specifications.where(spec).and(new ChartSpecifications<ChartStock>().histoTimeSpanEquals(histoPeriod));
			}
		}
		else{
			if(intradayWidth != null){
				spec = Specifications.where(spec).and(new ChartSpecifications<ChartStock>().intradayWidthEquals(intradayWidth));
			}
			if(intradayHeight != null){
				spec = Specifications.where(spec).and(new ChartSpecifications<ChartStock>().intradayHeightEquals(intradayHeight));
			}
		}
		
		spec = Specifications.where(spec).and(new ChartSpecifications<ChartStock>().indexEquals(index));
		return chartStockRepository.findAll(spec).stream().findFirst().orElse(null);
	}
	
	private StockProduct syncProduct(StockProduct stockProduct, StockQuote quote){
		stockProduct.setHigh(BigDecimal.valueOf(quote.getHigh()));
		stockProduct.setLow(BigDecimal.valueOf(quote.getLow()));
		stockProduct.setDailyLatestChange(BigDecimal.valueOf(quote.getLastChange()));
		stockProduct.setDailyLatestChangePercent(BigDecimal.valueOf(quote.getLastChangePercent()));
		stockProduct.setDailyLatestValue(BigDecimal.valueOf(quote.getLast()));
		stockProduct.setPreviousClose(BigDecimal.valueOf(quote.getPreviousClose()));
		stockProduct.setOpen(BigDecimal.valueOf(quote.getOpen()));
		if(!StringUtils.isEmpty(quote.getExchange())){
			stockProduct.setExchange(exchangeService.get(quote.getExchange()));
		}
		if(!StringUtils.isEmpty(quote.getCurrency())){
			stockProduct.setCurrency(quote.getCurrency());
		}
		stockProduct.setQuote(quote);
		return stockProduct;
	}

}
