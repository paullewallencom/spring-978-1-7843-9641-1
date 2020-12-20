/***
 *  Cloudstreetmarket.com is a Spring MVC showcase application developed 
 *  with the book Spring MVC Cookbook [PACKT] (2015). 
 * 	Copyright (C) 2015  Alex Bretet
 *  
 *  This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 **/
package edu.zipcloud.cloudstreetmarket.core.converters;

import org.springframework.core.convert.converter.Converter;
import org.springframework.social.yahoo.module.YahooQuote;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import edu.zipcloud.cloudstreetmarket.core.entities.StockQuote;

@Component
@Transactional(readOnly=true)
public class YahooQuoteToStockQuoteConverter implements Converter<YahooQuote, StockQuote> {

	@Override
	public StockQuote convert(YahooQuote yahooQuote) {
		StockQuote stockQuote = new StockQuote();
		stockQuote.setHigh(yahooQuote.getHigh());
		stockQuote.setLow(yahooQuote.getLow());
		stockQuote.setLastChange(yahooQuote.getLastChange());
		stockQuote.setLastChangePercent(yahooQuote.getLastChangePercent());
		stockQuote.setPreviousClose(yahooQuote.getPreviousClose());
		stockQuote.setOpen(yahooQuote.getOpen());
		stockQuote.setExchange(yahooQuote.getExchange());
		stockQuote.setCurrency(yahooQuote.getCurrency());
		return stockQuote;
	}
}