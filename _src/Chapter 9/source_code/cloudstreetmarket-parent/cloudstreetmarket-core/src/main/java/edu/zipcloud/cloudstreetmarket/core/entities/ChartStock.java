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
package edu.zipcloud.cloudstreetmarket.core.entities;

import static edu.zipcloud.cloudstreetmarket.core.entities.ChartStock.DISCR;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.springframework.social.yahoo.module.ChartHistoMovingAverage;
import org.springframework.social.yahoo.module.ChartHistoSize;
import org.springframework.social.yahoo.module.ChartHistoTimeSpan;
import org.springframework.social.yahoo.module.ChartType;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;

import edu.zipcloud.cloudstreetmarket.core.converters.IdentifiableSerializer;
import edu.zipcloud.cloudstreetmarket.core.converters.IdentifiableToIdConverter;

@Entity
@DiscriminatorValue(DISCR)
@XStreamAlias("chart_stock")
public class ChartStock extends Chart {

	private static final long serialVersionUID = 8405318980463119962L;

	public static final String DISCR = "stk";
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "stock_code")
	@JsonSerialize(using=IdentifiableSerializer.class)
	@JsonProperty("stockId")
	@XStreamConverter(value=IdentifiableToIdConverter.class, strings={"id"})
	@XStreamAlias("stockId")
	private StockProduct stock;

	public StockProduct getStock() {
		return stock;
	}

	public void setStock(StockProduct stock) {
		this.stock = stock;
	}
	
	public ChartStock(){
	}
	public ChartStock(Long id){
		super(id);
	}
	

	public ChartStock(StockProduct stock, ChartType type, ChartHistoSize histoSize, ChartHistoMovingAverage histoAverage, 
						ChartHistoTimeSpan histoPeriod, Integer intradayWidth, Integer intradayHeight, String path) {
		super(type, histoSize, histoAverage, histoPeriod, intradayWidth, intradayHeight, path);
		this.stock= stock;
	}

	@Override
	public String toString() {
		return "ChartStock [id=" + id + ", getType()=" + getType()
				+ ", getHistoTimeSpan()=" + getHistoTimeSpan()
				+ ", getHistoMovingAverage()=" + getHistoMovingAverage()
				+ ", getHistoSize()=" + getHistoSize()
				+ ", getIntradayWidth()=" + getIntradayWidth()
				+ ", getIntradayHeight()=" + getIntradayHeight()
				+ ", getInternalPath()=" + getInternalPath()
				+ ", getLastUpdate()=" + getLastUpdate() + ", getPath()="
				+ getPath() + "]";
	}
}
