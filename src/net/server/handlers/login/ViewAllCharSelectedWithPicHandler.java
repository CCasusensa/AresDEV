package net.server.handlers.login;

import java.net.InetAddress;
import java.net.UnknownHostException;

import net.AbstractMaplePacketHandler;
import net.server.Server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.MaplePacketCreator;
import tools.Randomizer;
import tools.data.input.SeekableLittleEndianAccessor;
import client.MapleClient;

public class ViewAllCharSelectedWithPicHandler extends
		AbstractMaplePacketHandler {

	private static Logger log = LoggerFactory
			.getLogger(ViewAllCharSelectedWithPicHandler.class);

	@Override
	public void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {

		final String pic = slea.readMapleAsciiString();
		final int charId = slea.readInt();
		final int world = slea.readInt();// world
		c.setWorld(world);
		final int channel = Randomizer.rand(0,
				Server.getInstance().getWorld(world).getChannels().size());
		c.setChannel(channel);
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
			} catch (final UnknownHostException e) {
			}

		} else {
			c.announce(MaplePacketCreator.wrongPic());
		}
	}
}
