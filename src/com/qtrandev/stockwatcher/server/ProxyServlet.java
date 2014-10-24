package com.qtrandev.stockwatcher.server;

/**
 *    Copyright 2010 Kenneth Jorgensen (kennethjorgensen.com) (modifications)
 *    - original code can be found at http://www.siafoo.net/snippet/258
 *    Copyright 2009 Stou Sandalski (Siafoo.net)
 *    Copyright 1999-2008 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * 
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
* This servlet provides a proxy to other servers for use in development with GWT devmode.
* It is not meant, in any way shape or form, to be used in production.
*/
public class ProxyServlet extends HttpServlet {
	
	//private static final String targetServer = "http://www.google.com/ig/api";
	private static final String targetServer = "http://finance.yahoo.com/d/quotes.csv";
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// http://www.sencha.com/blog/build-an-app-with-ext-gwt-dataproxies/
		// Create new client to perform the proxied request
		try {
			StringBuffer query = new StringBuffer();
			Enumeration<String> e = req.getParameterNames();
			while (e.hasMoreElements()) {
				String s = e.nextElement();
				if (s != null) {
					query.append(s);
					query.append("=");
					query.append(req.getParameter(s));
					query.append("&f=sbc1");
				}
			}
			URL url = new URL(targetServer+"?" + query.toString());
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String decodedString;
			StringBuffer sb = new StringBuffer();
			while ((decodedString = in.readLine()) != null) {
				sb.append(decodedString);
			}
			in.close();
			resp.getOutputStream().print(sb.toString());
		} catch (Exception e) {
			System.out.println("EXCEPTION WITH SERVLET");
		}
	}
}