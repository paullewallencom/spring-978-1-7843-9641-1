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
package edu.zipcloud.cloudstreetmarket.api.resources;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import edu.zipcloud.cloudstreetmarket.core.entities.CurrencyExchange;

@XStreamAlias("resource")
public class CurrencyExchangeResource extends Resource<CurrencyExchange> {
	
	public static final String CURRENCY_EXCHANGE = "currencyX";
	public static final String CURRENCY_EXCHANGES = "currencyX";
	public static final String CURRENCY_EXCHANGES_PATH = "/currencyX";

	public CurrencyExchangeResource(CurrencyExchange content, Link... links) {
		super(content, links);
	}
}