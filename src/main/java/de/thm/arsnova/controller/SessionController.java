/*
 * Copyright (C) 2012 THM webMedia
 * 
 * This file is part of ARSnova.
 *
 * ARSnova is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ARSnova is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.thm.arsnova.controller;

import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import de.thm.arsnova.entities.Feedback;
import de.thm.arsnova.entities.Question;
import de.thm.arsnova.entities.LoggedIn;
import de.thm.arsnova.entities.Session;
import de.thm.arsnova.entities.User;
import de.thm.arsnova.services.ISessionService;
import de.thm.arsnova.services.IUserService;
import de.thm.arsnova.socket.ARSnovaSocketIOServer;

@Controller
public class SessionController extends AbstractController {
	
	public static final Logger logger = LoggerFactory.getLogger(SessionController.class);
	
	@Autowired
	ISessionService sessionService;
	
	@Autowired
	IUserService userService;
	
	@Autowired
	ARSnovaSocketIOServer server;
	
	@RequestMapping(method = RequestMethod.POST, value = "/authorize")
	public void authorize(@RequestBody Object sessionObject, HttpServletResponse response) {
		String socketid = (String) JSONObject.fromObject(sessionObject).get("session");
		if(socketid == null) {
			return;
		}		
		User u = userService.getCurrentUser();
		logger.info("authorize session: " + socketid + ", user is:  " + u);
		response.setStatus(u != null ? HttpStatus.CREATED.value() : HttpStatus.UNAUTHORIZED.value());
		userService.putUser2SessionID(UUID.fromString(socketid), u);
	}
	
	@RequestMapping(value="/session/{sessionkey}", method=RequestMethod.GET)
	@ResponseBody
	public Session joinSession(@PathVariable String sessionkey) {
		return sessionService.joinSession(sessionkey);
	}
	
	@RequestMapping(value="/session/{sessionkey}/online", method=RequestMethod.POST)
	@ResponseBody
	public LoggedIn registerAsOnlineUser(@PathVariable String sessionkey, HttpServletResponse response) {
		User user = userService.getCurrentUser();
		LoggedIn loggedIn = sessionService.registerAsOnlineUser(user, sessionkey);
		if (loggedIn != null) {
			response.setStatus(HttpStatus.CREATED.value());
			return loggedIn;
		}
		
		response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
		return null;
	}
	
	@RequestMapping(value="/session", method=RequestMethod.POST)
	@ResponseBody
	public Session postNewSession(@RequestBody Session session, HttpServletResponse response) {
		Session newSession = sessionService.saveSession(session);
		if (session != null) {
			response.setStatus(HttpStatus.CREATED.value());
			return newSession;
		}

		response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
		return null;
	}

	@RequestMapping(value="/socketurl", method=RequestMethod.GET)
	@ResponseBody
	public String getSocketUrl() {
		StringBuilder url = new StringBuilder();
		
		url.append(server.isUseSSL() ? "https://" : "http://");
		url.append(server.getHostIp() + ":" + server.getPortNumber());
		
		return url.toString();
	}
	
	@RequestMapping(value={"/mySessions","/session/mysessions"}, method=RequestMethod.GET)
	@ResponseBody
	public List<Session> getMySession(HttpServletResponse response) {
		String username = userService.getCurrentUser().getUsername();
		if(username == null) {
			response.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		List<Session> sessions = sessionService.getMySessions(username);
		if (sessions == null || sessions.isEmpty()) {
			response.setStatus(HttpStatus.NOT_FOUND.value());
			return null;
		}
		return sessions;
	}
}
