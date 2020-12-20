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
package edu.zipcloud.cloudstreetmarket.api.controllers;

import static edu.zipcloud.cloudstreetmarket.api.resources.CurrencyExchangeResource.*;
import static org.springframework.web.bind.annotation.RequestMethod.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import edu.zipcloud.cloudstreetmarket.api.assemblers.CurrencyExchangeResourceAssembler;
import edu.zipcloud.cloudstreetmarket.api.resources.CurrencyExchangeResource;
import edu.zipcloud.cloudstreetmarket.api.services.CurrencyExchangeServiceOnline;
import edu.zipcloud.cloudstreetmarket.core.entities.CurrencyExchange;

@Api(value = CURRENCY_EXCHANGE, description = "Currency Exchanges") // Swagger annotation
@RestController
@ExposesResourceFor(CurrencyExchange.class)
@RequestMapping(value=CURRENCY_EXCHANGES_PATH, produces={"application/xml", "application/json"})
public class CurrencyExchangeController extends CloudstreetApiWCI<CurrencyExchange>{
	
	@Autowired
	private CurrencyExchangeServiceOnline currencyExchangeService;
	
	@Autowired
	private CurrencyExchangeResourceAssembler assembler;

	@RequestMapping(value="/{currencyX:[a-zA-Z=]+}{extension:\\.[a-z]+}", method=GET)
	@ApiOperation(value = "Get an overviews of one index", notes = "Return an index-overview")
	public CurrencyExchangeResource get(@PathVariable(value="currencyX") String ticker, @PathVariable(value="extension") String extension){
		return assembler.toResource(currencyExchangeService.gather(ticker));
	}
}