package net.server.handlers.login;

import java.net.InetAddress;
import java.net.UnknownHostException;

import net.AbstractMaplePacketHandler;
import net.server.Server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import client.MapleClient;

public final class RegisterPicHandler extends AbstractMaplePacketHandler {

	private static Logger log = LoggerFactory
			.getLogger(RegisterPicHandler.class);

	@Override
	public final void handlePacket(SeekableLittleEndianAccessor slea,
			MapleClient c) {
		slea.readByte();
		final int charId = slea.readInt();
		final String macs = slea.readMapleAsciiString();
		c.updateMacs(macs);
		if (c.hasBannedMac()) {
			c.getSession().close(true);
			return;
		}
		slea.readMapleAsciiString();
		final String pic = slea.readMapleAsciiString();
		if ((c.getPic() == null) || c.getPic().equals("")) {
			c.setPic(pic);
			if (c.getIdleTask() != null) {
				c.getIdleTask().cancel(true);
			}
			c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
			final String[] socket = Server.getInstance()
					.getIP(c.getWorld(), c.getChannel()).split(":");
			try {
				c.announce(MaplePacketCreator.getServerIP(
						InetAddress.getByName(socket[0]),
						Integer.parseInt(socket[1]), charId));
			} catch (final UnknownHostException e) {
			}
		} else {
			c.getSession().close(true);
		}
	}
}