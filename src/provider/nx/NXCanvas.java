package provider.nx;

import java.awt.image.BufferedImage;

import provider.MapleCanvas;
import provider.nx.core.format.nodes.NXBitmapNode;

/**
 * @author Aaron
 * @version 1.0
 * @since 6/8/13
 */
public class NXCanvas implements MapleCanvas {
	private final NXBitmapNode bitmapNode;
	private BufferedImage cache = null;

	public NXCanvas(NXBitmapNode bitmapNode) {
		this.bitmapNode = bitmapNode;
	}

	@Override
	public int getHeight() {
		this.ensureCached();
		return this.cache.getHeight();
	}

	@Override
	public int getWidth() {
		this.ensureCached();
		return this.cache.getWidth();
	}

	@Override
	public BufferedImage getImage() {
		this.ensureCached();
		return this.cache;
	}

	private void ensureCached() {
		if (this.cache == null) {
			this.cache = this.bitmapNode.getImage();
		}
	}
}
