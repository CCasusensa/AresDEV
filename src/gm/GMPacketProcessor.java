package gm;

import gm.server.handler.ChatHandler;
import gm.server.handler.CommandHandler;
import gm.server.handler.LoginHandler;
import gm.server.handler.PlayerListHandler;

/**
 *
 * @author kevintjuh93
 */
public final class GMPacketProcessor {
	private GMPacketHandler[] handlers;

	public GMPacketProcessor() {
		int maxRecvOp = 0;
		for (final GMRecvOpcode op : GMRecvOpcode.values()) {
			if (op.getValue() > maxRecvOp) {
				maxRecvOp = op.getValue();
			}
		}
		this.handlers = new GMPacketHandler[maxRecvOp + 1];
		this.reset();
	}

	public GMPacketHandler getHandler(short packetId) {
		if (packetId > this.handlers.length) {
			return null;
		}
		final GMPacketHandler handler = this.handlers[packetId];
		if (handler != null) {
			return handler;
		}
		return null;
	}

	public void registerHandler(GMRecvOpcode code, GMPacketHandler handler) {
		try {
			this.handlers[code.getValue()] = handler;
		} catch (final ArrayIndexOutOfBoundsException e) {
			System.out.println("Error registering handler - " + code.name());
		}
	}

	public void reset() {
		this.handlers = new GMPacketHandler[this.handlers.length];
		this.registerHandler(GMRecvOpcode.LOGIN, new LoginHandler());
		this.registerHandler(GMRecvOpcode.GM_CHAT, new ChatHandler());
		this.registerHandler(GMRecvOpcode.PLAYER_LIST, new PlayerListHandler());
		this.registerHandler(GMRecvOpcode.COMMAND, new CommandHandler());
	}
}
