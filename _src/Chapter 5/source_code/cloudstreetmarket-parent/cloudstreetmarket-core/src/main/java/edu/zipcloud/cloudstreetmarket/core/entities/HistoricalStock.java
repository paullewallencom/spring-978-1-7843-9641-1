package edu.zipcloud.cloudstreetmarket.core.entities;

import java.io.Serializable;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
@DiscriminatorValue("stk")
public class HistoricalStock extends Historic implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -802306391915956578L;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "stock_code")
	private StockProduct stock;

	private double bid;
	
	private double ask;
	
	public StockProduct getStock() {
		return stock;
	}

	public void setStock(StockProduct stock) {
		this.stock = stock;
	}

	public double getBid() {
		return bid;
	}

	public void setBid(double bid) {
		this.bid = bid;
	}

	public double getAsk() {
		return ask;
	}

	public void setAsk(double ask) {
		this.ask = ask;
	}
}
