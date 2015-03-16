package net.server.handlers.login;

import java.net.InetAddress;
import java.net.UnknownHostException;

import net.AbstractMaplePacketHandler;
import net.server.Server;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;
import client.MapleClient;

public class CharSelectedWithPicHandler extends AbstractMaplePacketHandler {

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {

		final String pic = slea.readMapleAsciiString();
		final int charId = slea.readInt();
		final String macs = slea.readMapleAsciiString();
		c.updateMacs(macs);

		if (c.hasBannedMac()) {
			c.getSession().close(true);
			return;
		}
		if (c.checkPic(pic)) {
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
			} catch (UnknownHostException | NumberFormatException e) {
			}
		} else {
			c.announce(MaplePacketCreator.wrongPic());
		}
	}
}
