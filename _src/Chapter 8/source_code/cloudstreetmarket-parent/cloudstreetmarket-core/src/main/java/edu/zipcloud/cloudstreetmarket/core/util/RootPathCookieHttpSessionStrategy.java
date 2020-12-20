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
package edu.zipcloud.cloudstreetmarket.core.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.springframework.session.Session;
import org.springframework.session.web.http.HttpSessionManager;
import org.springframework.session.web.http.MultiHttpSessionStrategy;

/**
 * A {@link HttpSessionStrategy} that uses a cookie to obtain the session from.
 * Specifically, this implementation will allow specifying a cookie name using
 * {@link CookieHttpSessionStrategy#setCookieName(String)}. The default is
 * "SESSION".
 *
 * When a session is created, the HTTP response will have a cookie with the
 * specified cookie name and the value of the session id. The cookie will be
 * marked as a session cookie, use the context path for the path of the cookie,
 * marked as HTTPOnly, and if
 * {@link javax.servlet.http.HttpServletRequest#isSecure()} returns true, the
 * cookie will be marked as secure. For example:
 *
 * <pre>
 * HTTP/1.1 200 OK
 * Set-Cookie: SESSION=f81d4fae-7dec-11d0-a765-00a0c91e6bf6; Path=/context-root; Secure; HttpOnly
 * </pre>
 *
 * The client should now include the session in each request by specifying the
 * same cookie in their request. For example:
 *
 * <pre>
 * GET /messages/ HTTP/1.1
 * Host: example.com
 * Cookie: SESSION=f81d4fae-7dec-11d0-a765-00a0c91e6bf6
 * </pre>
 *
 * When the session is invalidated, the server will send an HTTP response that
 * expires the cookie. For example:
 *
 * <pre>
 * HTTP/1.1 200 OK
 * Set-Cookie: SESSION=f81d4fae-7dec-11d0-a765-00a0c91e6bf6; Expires=Thur, 1 Jan 1970 00:00:00 GMT; Secure; HttpOnly
 * </pre>
 *
 * <h2>Supporting Multiple Simultaneous Sessions</h2>
 *
 * <p>
 * By default multiple sessions are also supported. Once a session is
 * established with the browser, another session can be initiated by specifying
 * a unique value for the {@link #setSessionAliasParamName(String)}. For
 * example, a request to:
 * </p>
 *
 * <pre>
 * GET /messages/?_s=1416195761178 HTTP/1.1
 * Host: example.com
 * Cookie: SESSION=f81d4fae-7dec-11d0-a765-00a0c91e6bf6
 * </pre>
 *
 * Will result in the following response:
 *
 * <pre>
 *  HTTP/1.1 200 OK
 * Set-Cookie: SESSION="0 f81d4fae-7dec-11d0-a765-00a0c91e6bf6 1416195761178 8a929cde-2218-4557-8d4e-82a79a37876d"; Expires=Thur, 1 Jan 1970 00:00:00 GMT; Secure; HttpOnly
 * </pre>
 *
 * <p>
 * To use the original session a request without the HTTP parameter u can be
 * made. To use the new session, a request with the HTTP parameter
 * _s=1416195761178 can be used. By default URLs will be rewritten to include the
 * currently selected session.
 * </p>
 *
 * <h2>Selecting Sessions</h2>
 *
 * <p>
 * Sessions can be managed by using the HttpSessionManager and
 * SessionRepository. If you are not using Spring in the rest of your
 * application you can obtain a reference from the HttpServletRequest
 * attributes. An example is provided below:
 * </p>
 *
 * {@code
 *      HttpSessionManager sessionManager =
 *              (HttpSessionManager) req.getAttribute(HttpSessionManager.class.getName());
 *      SessionRepository<Session> repo =
 *              (SessionRepository<Session>) req.getAttribute(SessionRepository.class.getName());
 *
 *      String currentSessionAlias = sessionManager.getCurrentSessionAlias(req);
 *      Map<String, String> sessionIds = sessionManager.getSessionIds(req);
 *      String newSessionAlias = String.valueOf(System.currentTimeMillis());
 *
 *      String contextPath = req.getContextPath();
 *      List<Account> accounts = new ArrayList<>();
 *      Account currentAccount = null;
 *      for(Map.Entry<String, String> entry : sessionIds.entrySet()) {
 *          String alias = entry.getKey();
 *          String sessionId = entry.getValue();
 *
 *          Session session = repo.getSession(sessionId);
 *          if(session == null) {
 *              continue;
 *          }
 *
 *          String username = session.getAttribute("username");
 *          if(username == null) {
 *              newSessionAlias = alias;
 *              continue;
 *          }
 *
 *          String logoutUrl = sessionManager.encodeURL("./logout", alias);
 *          String switchAccountUrl = sessionManager.encodeURL("./", alias);
 *          Account account = new Account(username, logoutUrl, switchAccountUrl);
 *          if(currentSessionAlias.equals(alias)) {
 *              currentAccount = account;
 *          } else {
 *              accounts.add(account);
 *          }
 *      }
 *
 *      req.setAttribute("currentAccount", currentAccount);
 *      req.setAttribute("addAccountUrl", sessionManager.encodeURL(contextPath, newSessionAlias));
 *      req.setAttribute("accounts", accounts);
 * }
 *
 *
 * @since 1.0
 * @author Rob Winch
 */
public final class RootPathCookieHttpSessionStrategy implements MultiHttpSessionStrategy, HttpSessionManager {
	private static final String SESSION_IDS_WRITTEN_ATTR = RootPathCookieHttpSessionStrategy.class.getName().concat(".SESSIONS_WRITTEN_ATTR");

	static final String DEFAULT_ALIAS = "0";

	static final String DEFAULT_SESSION_ALIAS_PARAM_NAME = "_s";

	private Pattern ALIAS_PATTERN = Pattern.compile("^[\\w-]{1,50}$");

	private String cookieName = "SESSION";

	private String sessionParam = DEFAULT_SESSION_ALIAS_PARAM_NAME;

	private boolean isServlet3Plus = isServlet3();

	public String getRequestedSessionId(HttpServletRequest request) {
		Map<String,String> sessionIds = getSessionIds(request);
		String sessionAlias = getCurrentSessionAlias(request);
		return sessionIds.get(sessionAlias);
	}

	public String getCurrentSessionAlias(HttpServletRequest request) {
		if(sessionParam == null) {
			return DEFAULT_ALIAS;
		}
		String u = request.getParameter(sessionParam);
		if(u == null) {
			return DEFAULT_ALIAS;
		}
		if(!ALIAS_PATTERN.matcher(u).matches()) {
			return DEFAULT_ALIAS;
		}
		return u;
	}

	public String getNewSessionAlias(HttpServletRequest request) {
		Set<String> sessionAliases = getSessionIds(request).keySet();
		if(sessionAliases.isEmpty()) {
			return DEFAULT_ALIAS;
		}
		long lastAlias = Long.decode(DEFAULT_ALIAS);
		for(String alias : sessionAliases) {
			long selectedAlias = safeParse(alias);
			if(selectedAlias > lastAlias) {
				lastAlias = selectedAlias;
			}
		}
		return Long.toHexString(lastAlias + 1);
	}

	private long safeParse(String hex) {
		try {
			return Long.decode("0x" + hex);
		} catch(NumberFormatException notNumber) {
			return 0;
		}
	}

	public void onNewSession(Session session, HttpServletRequest request, HttpServletResponse response) {
		Set<String> sessionIdsWritten = getSessionIdsWritten(request);
		if(sessionIdsWritten.contains(session.getId())) {
			return;
		}
		sessionIdsWritten.add(session.getId());

		Map<String,String> sessionIds = getSessionIds(request);
		String sessionAlias = getCurrentSessionAlias(request);
		sessionIds.put(sessionAlias, session.getId());
		Cookie sessionCookie = createSessionCookie(request, sessionIds);
		response.addCookie(sessionCookie);
	}

	@SuppressWarnings("unchecked")
	private Set<String> getSessionIdsWritten(HttpServletRequest request) {
		Set<String> sessionsWritten = (Set<String>) request.getAttribute(SESSION_IDS_WRITTEN_ATTR);
		if(sessionsWritten == null) {
			sessionsWritten = new HashSet<String>();
			request.setAttribute(SESSION_IDS_WRITTEN_ATTR, sessionsWritten);
		}
		return sessionsWritten;
	}

	private Cookie createSessionCookie(HttpServletRequest request,
			Map<String, String> sessionIds) {
		Cookie sessionCookie = new Cookie(cookieName,"");
		if(isServlet3Plus) {
			sessionCookie.setHttpOnly(true);
		}
		sessionCookie.setSecure(request.isSecure());
		sessionCookie.setPath(cookiePath(request));
		// TODO set domain?

		if(sessionIds.isEmpty()) {
			sessionCookie.setMaxAge(0);
			return sessionCookie;
		}

		if(sessionIds.size() == 1) {
			String cookieValue = sessionIds.values().iterator().next();
			sessionCookie.setValue(cookieValue);
			return sessionCookie;
		}
		StringBuffer buffer = new StringBuffer();
		for(Map.Entry<String,String> entry : sessionIds.entrySet()) {
			String alias = entry.getKey();
			String id = entry.getValue();

			buffer.append(alias);
			buffer.append(" ");
			buffer.append(id);
			buffer.append(" ");
		}
		buffer.deleteCharAt(buffer.length()-1);

		sessionCookie.setValue(buffer.toString());
		return sessionCookie;
	}

	public void onInvalidateSession(HttpServletRequest request, HttpServletResponse response) {
		Map<String,String> sessionIds = getSessionIds(request);
		String requestedAlias = getCurrentSessionAlias(request);
		sessionIds.remove(requestedAlias);

		Cookie sessionCookie = createSessionCookie(request, sessionIds);
		response.addCookie(sessionCookie);
	}

	/**
	 * Sets the name of the HTTP parameter that is used to specify the session
	 * alias. If the value is null, then only a single session is supported per
	 * browser.
	 *
	 * @param sessionAliasParamName
	 *            the name of the HTTP parameter used to specify the session
	 *            alias. If null, then ony a single session is supported per
	 *            browser.
	 */
	public void setSessionAliasParamName(String sessionAliasParamName) {
		this.sessionParam = sessionAliasParamName;
	}

	/**
	 * Sets the name of the cookie to be used
	 * @param cookieName the name of the cookie to be used
	 */
	public void setCookieName(String cookieName) {
		if(cookieName == null) {
			throw new IllegalArgumentException("cookieName cannot be null");
		}
		this.cookieName = cookieName;
	}

	/**
	 * Retrieve the first cookie with the given name. Note that multiple
	 * cookies can have the same name but different paths or domains.
	 * @param request current servlet request
	 * @param name cookie name
	 * @return the first cookie with the given name, or {@code null} if none is found
	 */
	private static Cookie getCookie(HttpServletRequest request, String name) {
		if(request == null) {
			throw new IllegalArgumentException("request cannot be null");
		}
		Cookie cookies[] = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (name.equals(cookie.getName())) {
					return cookie;
				}
			}
		}
		return null;
	}

	private static String cookiePath(HttpServletRequest request) {
		return "/";
	}

	public Map<String,String> getSessionIds(HttpServletRequest request) {
		Cookie session = getCookie(request, cookieName);
		String sessionCookieValue = session == null ? "" : session.getValue();
		Map<String,String> result = new LinkedHashMap<String,String>();
		StringTokenizer tokens = new StringTokenizer(sessionCookieValue, " ");
		if(tokens.countTokens() == 1) {
			result.put(DEFAULT_ALIAS, tokens.nextToken());
			return result;
		}
		while(tokens.hasMoreTokens()) {
			String alias = tokens.nextToken();
			if(!tokens.hasMoreTokens()) {
				break;
			}
			String id = tokens.nextToken();
			result.put(alias, id);
		}
		return result;
	}

	public HttpServletRequest wrapRequest(HttpServletRequest request, HttpServletResponse response) {
		request.setAttribute(HttpSessionManager.class.getName(), this);
		return request;
	}

	public HttpServletResponse wrapResponse(HttpServletRequest request, HttpServletResponse response) {
		return new MultiSessionHttpServletResponse(response, request);
	}

	class MultiSessionHttpServletResponse extends HttpServletResponseWrapper {
		private final HttpServletRequest request;

		public MultiSessionHttpServletResponse(HttpServletResponse response, HttpServletRequest request) {
			super(response);
			this.request = request;
		}

		@Override
		public String encodeRedirectURL(String url) {
			url = super.encodeRedirectURL(url);
			return RootPathCookieHttpSessionStrategy.this.encodeURL(url, getCurrentSessionAlias(request));
		}

		@Override
		public String encodeURL(String url) {
			url = super.encodeURL(url);

			String alias = getCurrentSessionAlias(request);
			return RootPathCookieHttpSessionStrategy.this.encodeURL(url, alias);
		}
	}

	public String encodeURL(String url, String sessionAlias) {
		String encodedSessionAlias = urlEncode(sessionAlias);
		int queryStart = url.indexOf("?");
		boolean isDefaultAlias = DEFAULT_ALIAS.equals(encodedSessionAlias);
		if(queryStart < 0) {
			return isDefaultAlias ? url : url + "?" + sessionParam + "=" + encodedSessionAlias;
		}
		String path = url.substring(0, queryStart);
		String query = url.substring(queryStart + 1, url.length());
		String replacement = isDefaultAlias ? "" : "$1"+encodedSessionAlias;
		query = query.replaceFirst( "((^|&)" + sessionParam + "=)([^&]+)?", replacement);
		if(!isDefaultAlias && url.endsWith(query)) {
			// no existing alias
			if(!(query.endsWith("&") || query.length() == 0)) {
				query += "&";
			}
			query += sessionParam + "=" + encodedSessionAlias;
		}

		return path + "?" + query;
	}

	private String urlEncode(String value) {
		try {
			return URLEncoder.encode(value, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns true if the Servlet 3 APIs are detected.
	 * @return
	 */
	private boolean isServlet3() {
		try {
			ServletRequest.class.getMethod("startAsync");
			return true;
		} catch(NoSuchMethodException e) {}
		return false;
	}
}