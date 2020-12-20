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

import static edu.zipcloud.cloudstreetmarket.api.controllers.AbstractProductController.PRODUCT_PATH;
import static edu.zipcloud.cloudstreetmarket.api.resources.StockProductResource.STOCKS;
import static edu.zipcloud.cloudstreetmarket.api.resources.StockProductResource.STOCKS_PATH;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import net.kaczmarzyk.spring.data.jpa.domain.LikeIgnoreCase;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Or;
import net.kaczmarzyk.spring.data.jpa.web.annotation.Spec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mangofactory.swagger.annotations.ApiIgnore;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import edu.zipcloud.cloudstreetmarket.api.assemblers.StockProductResourceAssembler;
import edu.zipcloud.cloudstreetmarket.api.resources.StockProductResource;
import edu.zipcloud.cloudstreetmarket.api.services.StockProductServiceOnline;
import edu.zipcloud.cloudstreetmarket.core.entities.StockProduct;
import edu.zipcloud.cloudstreetmarket.core.enums.MarketId;

@Api(value = STOCKS, description = "Financial stocks") // Swagger annotation
@RestController
@ExposesResourceFor(StockProduct.class)
@RequestMapping(value=PRODUCT_PATH + STOCKS_PATH, produces={"application/xml", "application/json"})
public class StockProductController extends AbstractProductController<StockProduct>{
	
	@Autowired
	private StockProductServiceOnline stockProductService;
	
	@Autowired
	private StockProductResourceAssembler assembler;
	
	@RequestMapping(method=GET)
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(value = "Get overviews of stocks", notes = "Return a page of stock-overviews")
	public PagedResources<StockProductResource> getSeveral(
			@Or({
                @Spec(params="cn", path="id", spec=LikeIgnoreCase.class),
                @Spec(params="cn", path="name", spec=LikeIgnoreCase.class)}	
			) @ApiIgnore Specification<StockProduct> spec,
			@ApiParam(value="Exchange ID") @RequestParam(value="exchange", required=false) String exchangeId,
			@ApiParam(value="Index ID") @RequestParam(value="index", required=false) String indexId, 
			@ApiParam(value="Market ID") @RequestParam(value="market", required=false) MarketId marketId, 
            @ApiParam(value="Starts with filter") @RequestParam(value="sw", defaultValue="", required=false) String startWith, 
            @ApiParam(value="Contains filter") @RequestParam(value="cn", defaultValue="", required=false) String contain, 
            @ApiIgnore @PageableDefault(size=10, page=0, sort={"name"}, direction=Direction.ASC) Pageable pageable){
		return pagedAssembler.toResource(stockProductService.gather(indexId, exchangeId, marketId, startWith, spec, pageable), assembler);
	}
	
	@RequestMapping(value="/{id:[a-zA-Z0-9.-:]+}{extension:\\.[a-z]+}", method=GET)
	@ResponseStatus(HttpStatus.OK)
	@ApiOperation(value = "Get one stock-overview", notes = "Return one stock-overview")
	public StockProductResource get(@ApiParam(value="Stock id: CCH.L") @PathVariable(value="id") String stockProductId, @PathVariable(value="extension") String extension){
		return assembler.toResource(stockProductService.gather(stockProductId));
	}
}