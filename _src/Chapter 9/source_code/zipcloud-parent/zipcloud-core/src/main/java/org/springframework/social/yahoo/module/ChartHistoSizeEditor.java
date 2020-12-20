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
package org.springframework.social.yahoo.module;

import java.beans.PropertyEditorSupport;

public class ChartHistoSizeEditor extends PropertyEditorSupport {
    public void setAsText(String text) {
		try{
			 setValue(ChartHistoSize.valueOf(text));
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("The provided value for the chart size parameter is invalid!");
		}
    }
}
