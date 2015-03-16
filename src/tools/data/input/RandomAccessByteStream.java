/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package tools.data.input;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Provides an abstract layer to a byte stream. This layer can be accessed
 * randomly.
 *
 * @author Frz
 * @version 1.0
 * @since Revision 323
 */
public class RandomAccessByteStream implements SeekableInputStreamBytestream {
	private final RandomAccessFile raf;
	private long read = 0;

	public RandomAccessByteStream(RandomAccessFile raf) {
		super();
		this.raf = raf;
	}

	@Override
	public int readByte() {
		int temp;
		try {
			temp = this.raf.read();
			if (temp == -1) {
				throw new RuntimeException("EOF");
			}
			this.read++;
			return temp;
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void seek(long offset) throws IOException {
		this.raf.seek(offset);
	}

	@Override
	public long getPosition() throws IOException {
		return this.raf.getFilePointer();
	}

	@Override
	public long getBytesRead() {
		return this.read;
	}

	@Override
	public long available() {
		try {
			return this.raf.length() - this.raf.getFilePointer();
		} catch (final IOException e) {
			System.out.println("ERROR " + e);
			return 0;
		}
	}
}
