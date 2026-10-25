package us.bringardner.parley.brand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Image;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ParleyBrandTest {

	@Test
	void everyListedFileIsInTheJar() throws IOException {
		for (String name : ParleyBrand.fileNames()) {
			URL url = ParleyBrand.resource(name);
			assertNotNull(url, name);
			try (InputStream in = url.openStream()) {
				assertTrue(in.read() >= 0, name + " is empty");
			}
		}
	}

	@Test
	void fileNamesIsUnmodifiable() {
		assertThrows(UnsupportedOperationException.class, () -> ParleyBrand.fileNames().add("x"));
	}

	@Test
	void appIconsHaveEverySizeInOrder() {
		List<Image> icons = ParleyBrand.appIcons();
		assertEquals(ParleyBrand.ICON_SIZES.size(), icons.size());
		for (int i = 0; i < icons.size(); i++) {
			int size = ParleyBrand.ICON_SIZES.get(i);
			assertEquals(size, icons.get(i).getWidth(null));
			assertEquals(size, icons.get(i).getHeight(null));
		}
	}

	@Test
	void iconPicksTheSmallestSizeThatIsBigEnough() {
		assertEquals(16, ParleyBrand.icon(1).getWidth());
		assertEquals(16, ParleyBrand.icon(16).getWidth());
		assertEquals(32, ParleyBrand.icon(17).getWidth());
		assertEquals(128, ParleyBrand.icon(100).getWidth());
		assertEquals(512, ParleyBrand.icon(512).getWidth());
		assertEquals(512, ParleyBrand.icon(4096).getWidth());
	}

	@Test
	void iconRejectsSizesBelowOne() {
		assertThrows(IllegalArgumentException.class, () -> ParleyBrand.icon(0));
		assertThrows(IllegalArgumentException.class, () -> ParleyBrand.icon(-5));
	}

	@Test
	void badgedIconsHaveEverySize() {
		for (String b : ParleyBrand.BADGES) {
			List<Image> icons = ParleyBrand.appIcons(b);
			assertEquals(ParleyBrand.ICON_SIZES.size(), icons.size(), b);
			for (int i = 0; i < icons.size(); i++) {
				assertEquals(ParleyBrand.ICON_SIZES.get(i), icons.get(i).getWidth(null), b);
			}
			assertEquals(64, ParleyBrand.icon(50, b).getWidth(), b);
		}
	}

	@Test
	void badgedIconDiffersFromThePlainOne() {
		BufferedImage plain = ParleyBrand.icon(128);
		BufferedImage files = ParleyBrand.icon(128, "files");
		// the badge sits in the bottom right corner: gold on the badge, black background on the plain icon
		assertTrue(plain.getRGB(97, 77) != files.getRGB(97, 77));
	}

	@Test
	void nullBadgeIsThePlainIcon() {
		assertEquals(ParleyBrand.icon(32).getRGB(16, 16), ParleyBrand.icon(32, null).getRGB(16, 16));
		assertEquals(ParleyBrand.ICON_SIZES.size(), ParleyBrand.appIcons(null).size());
	}

	@Test
	void unknownBadgesAreRejected() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ParleyBrand.appIcons("cats"));
		assertTrue(e.getMessage().contains("cats"));
		assertThrows(IllegalArgumentException.class, () -> ParleyBrand.icon(32, "FILES"));
	}

	@Test
	void eachCallReturnsANewImage() {
		assertNotSame(ParleyBrand.icon(32), ParleyBrand.icon(32));
	}

	@Test
	void heroAndMargotLoad() {
		BufferedImage hero = ParleyBrand.hero();
		assertEquals(960, hero.getWidth());
		assertEquals(1680, hero.getHeight());
		BufferedImage m = ParleyBrand.margot();
		assertEquals(720, m.getWidth());
		assertEquals(600, m.getHeight());
	}

	@Test
	void margotHasACardForEveryProtocolInAnyCase() {
		for (String p : ParleyBrand.PROTOCOLS) {
			assertEquals(640, ParleyBrand.margot(p).getWidth(), p);
			assertEquals(640, ParleyBrand.margot(p.toUpperCase()).getHeight(), p);
		}
	}

	@Test
	void margotRejectsUnknownProtocols() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ParleyBrand.margot("gopher"));
		assertTrue(e.getMessage().contains("gopher"));
		assertThrows(IllegalArgumentException.class, () -> ParleyBrand.margot(null));
	}

	@Test
	void resourceReturnsNullForMissingFiles() {
		assertNull(ParleyBrand.resource("no-such-file.png"));
	}

	@Test
	void loadReportsAMissingResource() {
		IllegalStateException e = assertThrows(IllegalStateException.class, () -> ParleyBrand.load("no-such-file.png"));
		assertTrue(e.getMessage().contains("no-such-file.png"));
	}

	@Test
	void loadWrapsUnreadableImages() {
		// An SVG is in the jar but is not something ImageIO can decode.
		UncheckedIOException e = assertThrows(UncheckedIOException.class, () -> ParleyBrand.load("parley-icon.svg"));
		assertTrue(e.getMessage().contains("parley-icon.svg"));
	}

	@Test
	void readRejectsNonImages() {
		InputStream in = new ByteArrayInputStream("not an image".getBytes(StandardCharsets.US_ASCII));
		assertThrows(IOException.class, () -> ParleyBrand.read(in, "text.txt"));
	}

	@Test
	void paletteMatchesTheArtwork() {
		assertEquals(0x080808, ParleyBrand.INK.getRGB() & 0xFFFFFF);
		assertEquals(0xD0AA42, ParleyBrand.GOLD_BRIGHT.getRGB() & 0xFFFFFF);
		assertEquals(0xBA983D, ParleyBrand.GOLD.getRGB() & 0xFFFFFF);
		assertEquals(0xAB8C3C, ParleyBrand.GOLD_MID.getRGB() & 0xFFFFFF);
		assertEquals(0x876D29, ParleyBrand.GOLD_DARK.getRGB() & 0xFFFFFF);
		assertEquals(0x372C0F, ParleyBrand.BRONZE.getRGB() & 0xFFFFFF);
	}

	/**
	 * The only test that calls installDefaultWindowIcons (it installs a listener for the whole
	 * JVM): a bad badge is rejected until it is installed, and after that every call does nothing.
	 */
	@Test
	void installDefaultWindowIconsWorksOnce() {
		assertThrows(IllegalArgumentException.class, () -> ParleyBrand.installDefaultWindowIcons("no-such-badge"));
		ParleyBrand.installDefaultWindowIcons(null);
		ParleyBrand.installDefaultWindowIcons(null);
		// already installed, so the badge is not even looked at
		ParleyBrand.installDefaultWindowIcons("no-such-badge");
	}

	/** A stand-in for java.awt.Window, which cannot be created headless. */
	static class FakeWindow {
		final FakeWindow owner;
		List<Image> icons = new ArrayList<>();

		FakeWindow(FakeWindow owner) {
			this.owner = owner;
		}
	}

	private static final List<Image> ICONS = List.of(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));

	private static boolean apply(int id, Object source) {
		return ParleyBrand.applyDefaultIcons(id, source, FakeWindow.class,
				w -> w.owner, w -> w.icons, (w, i) -> w.icons = i, ICONS);
	}

	@Test
	void aWindowWithoutIconsGetsThem() {
		FakeWindow w = new FakeWindow(null);
		assertTrue(apply(WindowEvent.WINDOW_OPENED, w));
		assertSame(ICONS, w.icons);
	}

	@Test
	void onlyOpeningAWindowCounts() {
		FakeWindow w = new FakeWindow(null);
		assertFalse(apply(WindowEvent.WINDOW_ACTIVATED, w));
		assertFalse(apply(WindowEvent.WINDOW_CLOSED, w));
		assertTrue(w.icons.isEmpty());
	}

	@Test
	void sourcesThatAreNotWindowsAreIgnored() {
		assertFalse(apply(WindowEvent.WINDOW_OPENED, "not a window"));
		assertFalse(apply(WindowEvent.WINDOW_OPENED, null));
	}

	@Test
	void aWindowWithItsOwnIconsKeepsThem() {
		FakeWindow w = new FakeWindow(null);
		List<Image> own = List.of(new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB));
		w.icons = own;
		assertFalse(apply(WindowEvent.WINDOW_OPENED, w));
		assertSame(own, w.icons);
	}

	@Test
	void aWindowWhoseOwnerChainHasIconsIsLeftAlone() {
		FakeWindow top = new FakeWindow(null);
		top.icons = List.of(new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB));
		FakeWindow middle = new FakeWindow(top);
		FakeWindow dialog = new FakeWindow(middle);
		assertFalse(apply(WindowEvent.WINDOW_OPENED, dialog));
		assertTrue(dialog.icons.isEmpty());
	}

	@Test
	void aWindowWhoseOwnersHaveNoIconsGetsThem() {
		FakeWindow dialog = new FakeWindow(new FakeWindow(new FakeWindow(null)));
		assertTrue(apply(WindowEvent.WINDOW_OPENED, dialog));
		assertSame(ICONS, dialog.icons);
		assertTrue(dialog.owner.icons.isEmpty(), "only the opened window changes");
	}
}
