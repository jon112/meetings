/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License") +  you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openmeetings.web.room;

import static org.apache.openmeetings.core.remote.ScopeApplicationAdapter.FLASH_NATIVE_SSL;
import static org.apache.openmeetings.core.remote.ScopeApplicationAdapter.FLASH_PORT;
import static org.apache.openmeetings.core.remote.ScopeApplicationAdapter.FLASH_SECURE;
import static org.apache.openmeetings.core.remote.ScopeApplicationAdapter.FLASH_SSL_PORT;
import static org.apache.openmeetings.util.OpenmeetingsVariables.webAppRootKey;
import static org.apache.openmeetings.web.app.Application.NAME_ATTR_KEY;
import static org.apache.openmeetings.web.app.Application.getBean;
import static org.apache.wicket.RuntimeConfigurationType.DEVELOPMENT;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.openmeetings.core.remote.ScopeApplicationAdapter;
import org.apache.openmeetings.db.dao.room.RoomDao;
import org.apache.openmeetings.db.dao.server.ISessionManager;
import org.apache.openmeetings.web.app.Application;
import org.apache.openmeetings.web.common.BasePanel;
import org.apache.openmeetings.web.common.OmAjaxClientInfoBehavior;
import org.apache.openmeetings.web.util.ExtendedClientProperties;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.PriorityHeaderItem;
import org.apache.wicket.protocol.http.request.WebClientInfo;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.mapper.parameter.PageParametersEncoder;
import org.apache.wicket.request.resource.JavaScriptResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.string.Strings;
import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import com.hazelcast.core.Member;

public class SwfPanel extends BasePanel {
	private static final long serialVersionUID = 1L;
	private final static Logger log = Red5LoggerFactory.getLogger(SwfPanel.class, webAppRootKey);
	public static final String SWF = "swf";
	public static final String SWF_TYPE_NETWORK = "network";
	public static final String SWF_TYPE_SETTINGS = "settings";
	private final PageParameters pp;

	public SwfPanel(String id) {
		this(id, new PageParameters());
	}

	public SwfPanel(String id, PageParameters pp) {
		super(id);
		this.pp = pp;
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();
		add(new OmAjaxClientInfoBehavior() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onClientInfo(AjaxRequestTarget target, WebClientInfo info) {
				super.onClientInfo(target, info);
				ExtendedClientProperties cp = (ExtendedClientProperties)info.getProperties();
				PageParameters spp = new PageParameters(pp);
				target.appendJavaScript(getInitFunction(spp, cp));
			}
		});
	}

	private static ResourceReference newResourceReference() {
		return new JavaScriptResourceReference(SwfPanel.class, "swf-functions.js");
	}

	@Override
	public void renderHead(IHeaderResponse response) {
		super.renderHead(response);
		response.render(new PriorityHeaderItem(JavaScriptHeaderItem.forReference(newResourceReference())));
	}

	public String getInitFunction(PageParameters pp, ExtendedClientProperties cp) {
		String initStr = null;
		StringValue type = pp.get(SWF);
		String swf = getFlashFile(type);
		if (!Strings.isEmpty(swf)) {
			String lbls = null;
			if (SWF_TYPE_NETWORK.equals(type.toString())) {
				lbls = getStringLabels(
						"network.test.ms", "network.test.mb", "network.test.sec"
						, "network.test.click.play", "network.test.copy.log"
						, "network.test.report", "network.test.report.start", "network.test.report.error"
						, "network.test.report.con.err"
						, "network.test.ping", "network.test.ping.avg", "network.test.ping.rcv"
						, "network.test.ping.lost", "network.test.ping.load"
						, "network.test.port", "network.test.port.avail", "network.test.port.stopped"
						, "network.test.jitter", "network.test.jitter.avg", "network.test.jitter.min"
						, "network.test.jitter.max"
						, "network.test.dwn", "network.test.dwn.bytes", "network.test.dwn.time"
						, "network.test.dwn.speed"
						, "network.test.upl", "network.test.upl.bytes", "network.test.upl.time"
						, "network.test.upl.speed"
						);
			} else if (SWF_TYPE_SETTINGS.equals(type.toString())) {
				lbls = getStringLabels("448", "449", "450", "451", "758", "447", "52", "53", "1429", "1430"
						, "775", "452", "767", "764", "765", "918", "54", "761", "762", "144", "203", "642"
						, "save.success");
			}
			JSONObject options = new JSONObject().put("src", swf + new PageParametersEncoder().encodePageParameters(pp));
			options.put("wmode", cp.isBrowserInternetExplorer() && cp.getBrowserVersionMajor() == 11 ? "opaque" : "direct");

			JSONObject s = new JSONObject();
			try {
				URL url = new URL(cp.getCodebase());
				String path = url.getPath();
				path = path.substring(1, path.indexOf('/', 2) + 1);
				JSONObject gs = getBean(ScopeApplicationAdapter.class).getFlashSettings();
				s.put("flashProtocol", gs.getBoolean(FLASH_SECURE) ? "rtmps" : "rtmp")
						.put("flashPort", gs.getBoolean(FLASH_SECURE) ? gs.getString(FLASH_SSL_PORT) : gs.getString(FLASH_PORT))
						.put("proxy", gs.getBoolean(FLASH_NATIVE_SSL) ? "best" : "none")
						.put("httpProtocol", url.getProtocol())
						.put("httpPort", url.getPort())
						.put("host", url.getHost())
						.put("path", path);
			} catch (Exception e) {
				log.error("Error while constructing video settings parameters", e);
			}
			initStr = String.format("labels = %s; config = %s; initSwf(%s);", lbls, s, options.toString());
		}
		return initStr;
	}

	private String getFlashFile(StringValue type) {
		String fmt;
		if (SWF_TYPE_SETTINGS.equals(type.toString())) {
			fmt = "main%s.swf11.swf";
		} else if (SWF_TYPE_NETWORK.equals(type.toString())) {
			fmt = "networktesting%s.swf10.swf";
		} else {
			return "";
		}
		return String.format(fmt, DEVELOPMENT == getApplication().getConfigurationType() ? "debug" : "");
	}

	public static String getStringLabels(String... ids) {
		JSONArray arr = new JSONArray();
		for (String id : ids) {
			arr.put(new JSONObject().put("id", id).put("value", Application.getString(id)));
		}
		return arr.toString();
	}

	private static PageParameters addServer(PageParameters pp, Member m) {
		return pp.add("host", m.getAddress().getHost());
	}

	private static PageParameters addServer(Long roomId, boolean addBasic) {
		PageParameters pp = new PageParameters();
		if (addBasic) {
			//pp.add("wicketsid", getSid()).add(WICKET_ROOM_ID, roomId).add("language", getLanguage());
		}

		long minimum = -1;
		Member result = null;
		Map<Member, Set<Long>> activeRoomsMap = new HashMap<>();
		List<Member> servers = Application.get().getServers();
		if (servers.size() > 1) {
			for (Member m : servers) {
				String serverId = m.getStringAttribute(NAME_ATTR_KEY);
				Set<Long> roomIds = getBean(ISessionManager.class).getActiveRoomIds(serverId);
				if (roomIds.contains(roomId)) {
					// if the room is already opened on a server, redirect the user to that one,
					log.debug("Room is already opened on a server {}", m.getAddress());
					return addServer(pp, m);
				}
				activeRoomsMap.put(m, roomIds);
			}
			for (Map.Entry<Member, Set<Long>> entry : activeRoomsMap.entrySet()) {
				Set<Long> roomIds = entry.getValue();
				long capacity = getBean(RoomDao.class).getRoomsCapacityByIds(roomIds);
				if (minimum < 0 || capacity < minimum) {
					minimum = capacity;
					result = entry.getKey();
				}
				log.debug("Checking server: {} Number of rooms {} RoomIds: {} max(Sum): {}", entry.getKey(), roomIds.size(), roomIds, capacity);
			}
		}
		return result == null ? pp : addServer(pp, result);
	}
}
