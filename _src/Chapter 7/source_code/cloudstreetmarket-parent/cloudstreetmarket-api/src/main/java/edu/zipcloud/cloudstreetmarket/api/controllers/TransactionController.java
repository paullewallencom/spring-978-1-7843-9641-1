package edu.zipcloud.cloudstreetmarket.api.controllers;

import static edu.zipcloud.cloudstreetmarket.api.resources.TransactionResource.*;
import static org.springframework.web.bind.annotation.RequestMethod.*;
import static edu.zipcloud.cloudstreetmarket.core.i18n.I18nKeys.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mangofactory.swagger.annotations.ApiIgnore;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import edu.zipcloud.cloudstreetmarket.api.assemblers.TransactionResourceAssembler;
import edu.zipcloud.cloudstreetmarket.api.resources.TransactionResource;
import edu.zipcloud.cloudstreetmarket.api.services.CurrencyExchangeService;
import edu.zipcloud.cloudstreetmarket.core.entities.CurrencyExchange;
import edu.zipcloud.cloudstreetmarket.core.entities.Transaction;
import edu.zipcloud.cloudstreetmarket.core.enums.UserActivityType;
import edu.zipcloud.cloudstreetmarket.core.services.TransactionService;
import edu.zipcloud.cloudstreetmarket.core.util.ValidatorUtil;
import edu.zipcloud.cloudstreetmarket.core.validators.TransactionValidator;

@Api(value = TRANSACTIONS, description = "Transactions") // Swagger annotation
@RestController
@ExposesResourceFor(Transaction.class)
@RequestMapping(value=ACTIONS_PATH + TRANSACTIONS_PATH, produces={"application/xml", "application/json"})
public class TransactionController extends CloudstreetApiWCI<Transaction> {
	
	@Autowired
	private TransactionService transactionService;
	
	@Autowired
	private TransactionResourceAssembler assembler;
	
	@Autowired
	private CurrencyExchangeService currencyExchangeService;
	
	@Autowired
	private TransactionValidator validator;
	
	@RequestMapping(method=GET)
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(value = "Get the user transactions", notes = "Return the transactions of a user")
	public PagedResources<TransactionResource> search(
			@ApiParam(value="User id: WHATEVERKEY") @RequestParam(value="user", required=false) String userName,
			@ApiParam(value="Quote id: 123L") @RequestParam(value="quote:[\\d]+", required=false) Long quoteId,
			@ApiParam(value="Product ticker: FB") @RequestParam(value="ticker:[a-zA-Z0-9-:]+", required=false) String ticker,
			@ApiIgnore @PageableDefault(size=10, page=0, sort={"lastUpdate"}, direction=Direction.DESC) Pageable pageable,
			HttpServletResponse response
			){
		return pagedAssembler.toResource(transactionService.findBy(pageable, userName, quoteId, ticker), assembler);
	}
	
	@RequestMapping(value="/{id}", method=GET)
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(value = "Get a transaction", notes = "Return one transaction")
	public TransactionResource get(@ApiParam(value="Transaction id: 24") @PathVariable(value="id") Long transactionId){
		return assembler.toResource(transactionService.get(transactionId));
	}
	
	@RequestMapping(method=POST)
	@ResponseStatus(HttpStatus.CREATED)
	@ApiOperation(value = "Processes a transaction")
	public TransactionResource post(@Valid @RequestBody Transaction transaction, HttpServletResponse response, BindingResult result) {
		ValidatorUtil.raiseFirstError(result);
		transactionService.hydrate(transaction);
		
		if(!transaction.getUser().getUsername().equals(getPrincipal().getUsername())){
			throw new AccessDeniedException(bundle.get(I18N_TRANSACTIONS_USER_FORBIDDEN));
		}
		
		CurrencyExchange currencyExchange = null;
		
		if(!getAuthenticated().getCurrency().equals(transaction.getQuote().getSupportedCurrency())){
			currencyExchange = currencyExchangeService.gather(transaction.getQuote().getSupportedCurrency().name()+ getAuthenticated().getCurrency().name() + "=X");
		}

		if(transaction.getType().equals(UserActivityType.BUY)){
			if(communityService.isAffordableToUser(transaction.getQuantity(), transaction.getQuote(), transaction.getUser(), currencyExchange)){
				communityService.alterUserBalance(transaction.getQuantity(), transaction.getQuote(), transaction.getUser(), transaction.getType(), currencyExchange);
				transaction = transactionService.save(transaction);
			}
			else{
				throw new AccessDeniedException(bundle.get(I18N_TRANSACTIONS_CANT_AFFORD));
			}
		}
		else if(transaction.getType().equals(UserActivityType.SELL)){
			if(transactionService.isOwnedByUser(transaction.getUser(), transaction.getQuantity(), transaction.getQuote().getStock())){
				communityService.alterUserBalance(transaction.getQuantity(), transaction.getQuote(), transaction.getUser(), transaction.getType(), currencyExchange);
				transaction = transactionService.save(transaction);
			}
			else{
				throw new AccessDeniedException(bundle.get(I18N_TRANSACTIONS_DONT_OWN_QUANTITY));
			}
		}
		
		TransactionResource resource = assembler.toResource(transaction);
		response.setHeader(LOCATION_HEADER, resource.getLink("self").getHref());
		return resource;
	}
	
	@PreAuthorize("hasRole('ADMIN')")
	@RequestMapping(value="/{id}", method=DELETE)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@ApiOperation(value = "Delete a transaction", notes = "Delete one transaction")
	public void delete(@ApiParam(value="Transaction id: 24") @PathVariable(value="id") Long transactionId){
		transactionService.delete(transactionId);
	}
	
	@InitBinder
    protected void initBinder(WebDataBinder binder) {
        binder.setValidator(validator);
    }
}