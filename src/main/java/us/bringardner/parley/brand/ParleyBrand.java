/**
 * <PRE>
 *
 * Copyright 2026 <A href="http://bringardner.us/tony">Tony Bringardner</A>
 *
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       <A href="http://www.apache.org/licenses/LICENSE-2.0">http://www.apache.org/licenses/LICENSE-2.0</A>
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  </PRE>
 *
 *
 *	@author Tony Bringardner
 *
 *
 */
package us.bringardner.parley.brand;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

/**
 * The Parley logo, app icons and mascot, loaded from this jar.
 *
 * <p>Every method returns a new image, so callers may draw on or scale what they get without
 * affecting anyone else. The images are small; cache them yourself if you show one often.</p>
 *
 * <pre>
 * frame.setIconImages(ParleyBrand.appIcons());   // the OS picks the size it needs
 * Image about = ParleyBrand.hero();              // About box or splash screen
 * Image dns   = ParleyBrand.margot("dns");       // Margot asking "Parlez-vous DNS ?"
 * frame.setIconImages(ParleyBrand.appIcons("files"));   // with a folder badge, for a file browser
 * </pre>
 *
 * <p>The SVG originals are in the same package; {@link #resource(String)} returns their URLs.</p>
 */
public final class ParleyBrand {

	/** Where the images live inside the jar. */
	public static final String RESOURCE_PATH = "/us/bringardner/parley/brand/";

	/**
	 * The square app icon sizes in this jar, smallest first. 64 px and below are a plain
	 * silhouette; 128 px and up add the tower's lattice.
	 */
	public static final List<Integer> ICON_SIZES = List.of(16, 32, 48, 64, 128, 256, 512);

	/**
	 * The corner badges an application built with Parley can add to the app icon, so several
	 * Parley-branded applications can be told apart in the Dock or taskbar:
	 * {@code "files"} (a folder, for file browsers) and {@code "terminal"} ({@code >_}, for shells and IDEs).
	 */
	public static final List<String> BADGES = List.of("files", "terminal");

	/** The protocols Margot has a card for, as passed to {@link #margot(String)}. */
	public static final List<String> PROTOCOLS = List.of("dns", "pop3", "imap", "smtp", "ssh", "ftp", "sftp");

	/** Background black (#080808). */
	public static final Color INK = new Color(0x080808);
	/** Bright gold (#D0AA42): bolts, platforms, the wordmark and card backgrounds. */
	public static final Color GOLD_BRIGHT = new Color(0xD0AA42);
	/** Gold (#BA983D): Margot's outline. */
	public static final Color GOLD = new Color(0xBA983D);
	/** Mid gold (#AB8C3C): the body of the tower. */
	public static final Color GOLD_MID = new Color(0xAB8C3C);
	/** Dark gold (#876D29): radio waves and Margot's tail. */
	public static final Color GOLD_DARK = new Color(0x876D29);
	/** Deep bronze (#372C0F): lattice, cables and text on gold. */
	public static final Color BRONZE = new Color(0x372C0F);

	private ParleyBrand() {
	}

	/**
	 * All the app icon sizes, smallest first, ready for {@code Window.setIconImages(...)}.
	 *
	 * @return a new, modifiable list with one image per entry in {@link #ICON_SIZES}
	 */
	public static List<Image> appIcons() {
		return appIcons(null);
	}

	/**
	 * All the app icon sizes with a corner badge, smallest first, ready for
	 * {@code Window.setIconImages(...)}.
	 *
	 * @param badge one of {@link #BADGES}, or null for the plain icon
	 * @return a new, modifiable list with one image per entry in {@link #ICON_SIZES}
	 * @throws IllegalArgumentException if {@code badge} is not null and not one of {@link #BADGES}
	 */
	public static List<Image> appIcons(String badge) {
		String prefix = iconPrefix(badge);
		List<Image> ret = new ArrayList<>(ICON_SIZES.size());
		for (int size : ICON_SIZES) {
			ret.add(load(prefix + size + ".png"));
		}
		return ret;
	}

	private static boolean windowIconsInstalled;

	/**
	 * Gives every Swing window and dialog that has no icon of its own (and no owner with one) the
	 * Parley app icon, so dialogs never show the Java cup. Call once at startup; later calls do nothing.
	 *
	 * @param badge one of {@link #BADGES}, or null for the plain icon
	 * @throws IllegalArgumentException if {@code badge} is not null and not one of {@link #BADGES}
	 */
	public static synchronized void installDefaultWindowIcons(String badge) {
		if (windowIconsInstalled) {
			return;
		}
		final List<Image> icons = appIcons(badge);
		windowIconsInstalled = true;
		java.awt.Toolkit.getDefaultToolkit().addAWTEventListener(
				e -> applyDefaultIcons(e.getID(), e.getSource(), java.awt.Window.class,
						java.awt.Window::getOwner, java.awt.Window::getIconImages, java.awt.Window::setIconImages, icons),
				java.awt.AWTEvent.WINDOW_EVENT_MASK);
	}

	/**
	 * The decision behind {@link #installDefaultWindowIcons(String)}: when a window opens and
	 * neither it nor any owner up its chain has icons, give it {@code icons}. The window operations
	 * are passed in, so the tests can check every case headless, with stand-ins for windows.
	 *
	 * @param <W> the window type ({@link java.awt.Window} in real use)
	 * @param eventId the event's id; only {@link java.awt.event.WindowEvent#WINDOW_OPENED} is acted on
	 * @param source the event's source; anything that is not a {@code type} is ignored
	 * @param type the window class
	 * @param owner a window's owner, or null at the top of the chain
	 * @param iconsOf a window's current icon images
	 * @param setIcons sets a window's icon images
	 * @param icons the icons to give the window
	 * @return true if the window was given {@code icons}
	 */
	static <W> boolean applyDefaultIcons(int eventId, Object source, Class<W> type,
			java.util.function.Function<W, W> owner, java.util.function.Function<W, List<Image>> iconsOf,
			java.util.function.BiConsumer<W, List<Image>> setIcons, List<Image> icons) {
		if (eventId != java.awt.event.WindowEvent.WINDOW_OPENED || !type.isInstance(source)) {
			return false;
		}
		W w = type.cast(source);
		for (W o = w; o != null; o = owner.apply(o)) {
			if (!iconsOf.apply(o).isEmpty()) {
				return false;
			}
		}
		setIcons.accept(w, icons);
		return true;
	}

	/**
	 * The app icon at the smallest stored size that is at least {@code size} pixels,
	 * or the largest one if {@code size} is bigger than every stored size.
	 *
	 * @param size the width and height wanted, in pixels
	 * @return the icon
	 * @throws IllegalArgumentException if {@code size} is less than 1
	 */
	public static BufferedImage icon(int size) {
		return icon(size, null);
	}

	/**
	 * The app icon with a corner badge, at the smallest stored size that is at least
	 * {@code size} pixels, or the largest one if {@code size} is bigger than every stored size.
	 *
	 * @param size the width and height wanted, in pixels
	 * @param badge one of {@link #BADGES}, or null for the plain icon
	 * @return the icon
	 * @throws IllegalArgumentException if {@code size} is less than 1, or {@code badge} is not
	 *         null and not one of {@link #BADGES}
	 */
	public static BufferedImage icon(int size, String badge) {
		String prefix = iconPrefix(badge);
		if (size < 1) {
			throw new IllegalArgumentException("size must be at least 1, not " + size);
		}
		int pick = ICON_SIZES.get(ICON_SIZES.size() - 1);
		for (int s : ICON_SIZES) {
			if (s >= size) {
				pick = s;
				break;
			}
		}
		return load(prefix + pick + ".png");
	}

	private static String iconPrefix(String badge) {
		if (badge == null) {
			return "parley-icon-";
		}
		if (!BADGES.contains(badge)) {
			throw new IllegalArgumentException("No badge '" + badge + "'; expected null or one of " + BADGES);
		}
		return "parley-icon-" + badge + "-";
	}

	/**
	 * Margot perched on the Parley tower, with the wordmark, on black (960 x 1680 px).
	 *
	 * @return the image
	 */
	public static BufferedImage hero() {
		return load("parley-hero.png");
	}

	/**
	 * Margot the magpie on her own, on a transparent background (720 x 600 px).
	 *
	 * @return the image
	 */
	public static BufferedImage margot() {
		return load("margot.png");
	}

	/**
	 * Margot's card for one protocol: she asks "Parlez-vous DNS ?" and so on (640 x 640 px).
	 *
	 * @param protocol one of {@link #PROTOCOLS}, in any case
	 * @return the image
	 * @throws IllegalArgumentException if there is no card for {@code protocol}
	 */
	public static BufferedImage margot(String protocol) {
		String key = protocol == null ? "" : protocol.toLowerCase(Locale.ROOT);
		if (!PROTOCOLS.contains(key)) {
			throw new IllegalArgumentException("No Margot card for '" + protocol + "'; expected one of " + PROTOCOLS);
		}
		return load("margot-" + key + ".png");
	}

	/**
	 * The URL of a file in this jar's brand package, for example {@code "parley-icon.svg"}.
	 *
	 * @param fileName the file name, with no path
	 * @return the URL, or null if there is no such file
	 */
	public static URL resource(String fileName) {
		return ParleyBrand.class.getResource(RESOURCE_PATH + fileName);
	}

	/**
	 * The names of the image files in this jar, for tools that copy them somewhere else
	 * (a web site, say).
	 *
	 * @return an unmodifiable list of file names, PNGs and SVGs
	 */
	public static List<String> fileNames() {
		List<String> ret = new ArrayList<>();
		for (int size : ICON_SIZES) {
			ret.add("parley-icon-" + size + ".png");
		}
		ret.add("parley-icon.svg");
		ret.add("parley-icon-large.svg");
		for (String b : BADGES) {
			for (int size : ICON_SIZES) {
				ret.add("parley-icon-" + b + "-" + size + ".png");
			}
			ret.add("parley-icon-" + b + ".svg");
			ret.add("parley-icon-" + b + "-large.svg");
		}
		ret.add("parley-tower.svg");
		ret.add("parley-hero.png");
		ret.add("parley-hero.svg");
		ret.add("margot.png");
		ret.add("margot.svg");
		for (String p : PROTOCOLS) {
			ret.add("margot-" + p + ".png");
			ret.add("margot-" + p + ".svg");
		}
		return Collections.unmodifiableList(ret);
	}

	static BufferedImage load(String fileName) {
		URL url = resource(fileName);
		if (url == null) {
			throw new IllegalStateException("Missing brand resource " + RESOURCE_PATH + fileName);
		}
		try (InputStream in = url.openStream()) {
			return read(in, fileName);
		} catch (IOException e) {
			throw new UncheckedIOException("Can't read brand resource " + fileName, e);
		}
	}

	static BufferedImage read(InputStream in, String fileName) throws IOException {
		BufferedImage img = ImageIO.read(in);
		if (img == null) {
			throw new IOException(fileName + " is not an image ImageIO can read");
		}
		return img;
	}
}
