package rick.ward;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public class GameWindow extends JFrame {

	// =================
	// FROM GOOGLE
	// =================
	private BufferedImage getRotatedImage(BufferedImage src, double angleDegrees) {
		int w = src.getWidth();
		int h = src.getHeight();

		// Create a blank destination image matching the original type
		BufferedImage dest = new BufferedImage(w, h, src.getType());
		Graphics2D g2d = dest.createGraphics();

		// Set up rendering rules
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Rotate the canvas from the midpoint
		g2d.rotate(Math.toRadians(angleDegrees), w / 2.0, h / 2.0);
		g2d.drawImage(src, 0, 0, null);
		g2d.dispose();

		return dest;
	}

	// =================
	// UNTIL THIS POINT
	// =================
	private boolean mouseDown = false;
	private String cursorType = "clicker";
	private boolean foundSomething = false;
	private boolean areTheLightsOn = false;

	public String gameName;

	private boolean debugMode = true; // change to false before submitting

	private boolean pinCollected = false;
	private boolean anyDoorKeyIsBeingHeld = false;
	private boolean frontDoorKeyCollected = false;
	private boolean doorBellIsPressed = false;
	private boolean rockIsSearched = false;
	private boolean frontDoorOpen = false;

	private boolean areTheLightsOnInThePaintingRoom = false;

	private Font TimesNewHamster = new Font("Dialog", Font.BOLD, 10);
	private Font TimesNewHamsterPro = new Font("Dialog", Font.BOLD, 15);

	private int TimerAsOfLastClick = 0;
	private int TimerAsOfEffect = 0;
	private int TimerAsOfAmbience = 0;

	private String tooltip = "This is the address.";

	private static File knockSFX = new File("knock.wav");
	private static File doorSFX = new File("door.wav");
	private static File doorbellSFX = new File("doorbell.wav");
	private static File footstepsSFX = new File("footsteps.wav");
	private static File rockSFX = new File("slide.wav");
	private static File omenSFX = new File("omen.wav");

	private static File collectSFX = new File("collect.wav");

	private static File clickSFX = new File("click.wav");
	private static File thunderSFX = new File("thunder.wav");
	private static File rainAMB = new File("rain_amb.wav");

	private static double LatestRNG = 0;

	private static int metroGnome = 0;

	private static String currentScene = "outside";

	private boolean MagnifyingGlassActive = false;
	private boolean MagnifyingGlassHover = false;

	private BufferedImage bgImage;
	private BufferedImage uiButton0;
	private BufferedImage uiButton1;

	private BufferedImage goto_0;
	private BufferedImage goto_1;

	private BufferedImage overlay_doorbell;

	private BufferedImage room1;
	private BufferedImage room1_lightning;
	private BufferedImage room2;
	private BufferedImage room3;
	private BufferedImage room3_lightning;
	private BufferedImage room4;
	private BufferedImage outside0;
	private BufferedImage outside1;
	private BufferedImage outside2;

	private BufferedImage rock0;
	private BufferedImage rock1;
	private BufferedImage key0;

	private BufferedImage cursor0;
	private BufferedImage cursor1;
	private BufferedImage cursor2;

	private BufferedImage magnifying_cursor;
	private BufferedImage key_cursor;

	private BufferedImage magnifying_glass;
	private BufferedImage diary;

	private BufferedImage pin;
	// -------------------------------------------------------------------------
	// Config
	// -------------------------------------------------------------------------
	private static final int WIDTH = 800;
	private static final int HEIGHT = 600;
	private static final int TARGET_FPS = 60;

	// -------------------------------------------------------------------------
	// State
	// -------------------------------------------------------------------------
	private int mouseX = 0;
	private int mouseY = 0;

	// -------------------------------------------------------------------------
	// Canvas (all drawing happens here)
	// -------------------------------------------------------------------------
	private final Canvas canvas = new Canvas() {
		@Override
		public void paint(Graphics g) {
			/* handled by active rendering */ }
	};

	public GameWindow() {
		canvas.setFocusable(true);
		canvas.requestFocus();
		canvas.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int key = e.getKeyCode();
				if (key == KeyEvent.VK_1) {
					currentScene = "darkroom";
				} else if (key == KeyEvent.VK_2) {
					currentScene = "lightroom";
				} else if (key == KeyEvent.VK_3) {
					currentScene = "atrium";
				} else if (key == KeyEvent.VK_4) {
					currentScene = "hallway";
				} else if (key == KeyEvent.VK_5) {
					currentScene = "outside";
				} else if (key == KeyEvent.VK_I) {
					if (debugMode) {
						debugMode = false;
					} else if (!debugMode) {
						debugMode = true;
					}
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				// fired when key is let go
			}
		});
		setTitle(gameName);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setResizable(false);

		canvas.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				mouseDown = true;
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				mouseDown = false;
			}
		});

		canvas.setPreferredSize(new Dimension(WIDTH, HEIGHT));
		canvas.setIgnoreRepaint(true); // we drive all repaints ourselves
		add(canvas);
		pack();
		setLocationRelativeTo(null);

		// ── Hide the real cursor ──────────────────────────────────────────────
		Toolkit tk = Toolkit.getDefaultToolkit();
		BufferedImage blank = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Cursor hiddenCursor = tk.createCustomCursor(blank, new Point(0, 0), "hidden");
		canvas.setCursor(hiddenCursor);

		// ── Track mouse position ──────────────────────────────────────────────
//        canvas.addMouseMotionListener(new MouseMotionAdapter() {
//            @Override public void mouseMoved  (MouseEvent e) { mouseX = e.getX(); mouseY = e.getY(); }
//            @Override public void mouseDragged(MouseEvent e) { mouseX = e.getX(); mouseY = e.getY(); }
//        });
	}

	// =========================================================================
	// MAIN LOOP
	// =========================================================================
	public static void playWav(File file) {
		// Run this in a background thread to prevent GUI/Logic lag
		new Thread(() -> {
			try {
				AudioInputStream inputStream = AudioSystem.getAudioInputStream(file);
				Clip clip = AudioSystem.getClip();
				clip.open(inputStream);
				clip.start();

				// Optional: Clean up memory once the sound finishes
				clip.addLineListener(event -> {
					if (event.getType() == LineEvent.Type.STOP) {
						clip.close();
					}
				});

			} catch (Exception e) {
				System.err.println("Playback error: " + e.getMessage());
			}
		}).start();
	}

	private void run() throws IOException, LineUnavailableException, UnsupportedAudioFileException {
		setVisible(true);
		canvas.createBufferStrategy(2); // double-buffer

		final long nsPerFrame = 1_000_000_000L / TARGET_FPS;

		// ── Put any one-time setup here ───────────────────────────────────────
		setup();

		while (true) {
			long frameStart = System.nanoTime();

			// ── Update ────────────────────────────────────────────────────────

			update();

			// ── Render ───────────────────────────────────────────────────────
			do {
				Graphics2D g = (Graphics2D) canvas.getBufferStrategy().getDrawGraphics();
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				draw(g); // ← your drawing code lives here
				drawCursor(g); // always drawn last so it's on top

				g.dispose();
			} while (canvas.getBufferStrategy().contentsRestored());
			canvas.getBufferStrategy().show();

			// ── Cap frame rate ────────────────────────────────────────────────
			long elapsed = System.nanoTime() - frameStart;
			long sleepNs = nsPerFrame - elapsed;
			if (sleepNs > 0) {
				try {
					Thread.sleep(sleepNs / 1_000_000, (int) (sleepNs % 1_000_000));
				} catch (InterruptedException ignored) {
				}
			}
		}
	}

	// =========================================================================
	// OVERRIDE THESE THREE METHODS
	// =========================================================================

	/**
	 * Called once before the loop starts. Load assets, initialise state, etc.
	 * 
	 * @throws IOException
	 */
	private void setup() throws IOException {
		String[] Titles = { "Proffesor", "Doctor", "Inspector", "Detective", "Mr.", "Grand Inquisitor", "Dr.", "Sir",
				"Lord" };
		String[] ProperNouns = { "Blambry", "Cave Johnson", "Steve", "Gorbelstein", "Edlai", "Virgil", "Wheatley",
				"Nigel", "Stirling", "Conly", "Greg" };
		String[] Nouns = { "the Conundrum", "the Mystery", "his Dillema", "the Universe's Cube",
				"Doctor Langeskov's Invention", "Doctor Langeskov's Cube", "the Cube", "the King's Missing Socks" };
		String[] Catches = { "of Doom", "of Death", "to Infinity and Beyond", "of the Missing City",
				"of the Unclaimed Luggage", "to the Nth Degree", "Concerning Matters of National Security" };
		int TitlesRand = (int) (Math.random() * 9);
		int ProperNounsRand = (int) (Math.random() * 11);
		int NounsRand = (int) (Math.random() * 8);
		int CatchesRand = (int) (Math.random() * 7);
		String gameName = Titles[TitlesRand] + " " + ProperNouns[ProperNounsRand] + " and " + Nouns[NounsRand] + " "
				+ Catches[CatchesRand];
		setTitle(gameName);
		playWav(rainAMB);
		bgImage = ImageIO.read(new File("ui_scale4.png"));
		uiButton0 = ImageIO.read(new File("button_scale4_0.png"));
		uiButton1 = ImageIO.read(new File("button_scale4_1.png"));
		room1 = ImageIO.read(new File("darkroom_0_scale4.png"));
		room1_lightning = ImageIO.read(new File("darkroom_1_scale4.png"));
		room2 = ImageIO.read(new File("lightroom_scale4.png"));
		room3 = ImageIO.read(new File("room3.png"));

		outside0 = ImageIO.read(new File("front_yard_no_lightning.png"));
		outside1 = ImageIO.read(new File("front_yard_yes_lightning.png"));
		outside2 = ImageIO.read(new File("doors_open.png"));

		key_cursor = ImageIO.read(new File("key_cursor.png"));

		overlay_doorbell = ImageIO.read(new File("doorbell_pressed_overlay.png"));

		rock0 = ImageIO.read(new File("rock0.png"));
		rock1 = ImageIO.read(new File("rock1.png"));
		;
		key0 = ImageIO.read(new File("key_onGround.png"));
		;

		room3_lightning = ImageIO.read(new File("room3_lightning.png"));
		room4 = ImageIO.read(new File("room4.png"));
		cursor0 = ImageIO.read(new File("cursor_0.png"));
		cursor1 = ImageIO.read(new File("cursor_1.png"));
		cursor2 = ImageIO.read(new File("cursor_2.png"));
		magnifying_glass = ImageIO.read(new File("magnifying_glass.png"));
		magnifying_cursor = ImageIO.read(new File("cursor_magnifying.png"));
		goto_0 = ImageIO.read(new File("go_to_0_x16.png"));
		goto_1 = ImageIO.read(new File("go_to_1_x16.png"));
		pin = ImageIO.read(new File("collectable_room2_pin.png"));
		// TODO: your setup code
	}

	/**
	 * Called every frame before drawing. Move things, update physics, etc.
	 * 
	 * @throws UnsupportedAudioFileException
	 * @throws LineUnavailableException
	 */
	private boolean thundered = false;

	private void update() throws LineUnavailableException, UnsupportedAudioFileException {
		LatestRNG = Math.random();

		PointerInfo pi = MouseInfo.getPointerInfo();
		Point p = pi.getLocation();
		SwingUtilities.convertPointFromScreen(p, canvas);
		mouseX = p.x;
		mouseY = p.y;
		// TODO: your update/logic code
		if (mouseDown && mouseX > 270 && mouseY > 450 && mouseX < 430 && mouseY < 580 && MagnifyingGlassActive
				&& (TimerAsOfLastClick + 1) < metroGnome && !anyDoorKeyIsBeingHeld) {
			MagnifyingGlassActive = false;
			cursorType = "clicker";
			TimerAsOfLastClick = metroGnome;
		} else if (mouseDown && mouseX > 270 && mouseY > 450 && mouseX < 430 && mouseY < 580 && !MagnifyingGlassActive
				&& (TimerAsOfLastClick + 1) < metroGnome && !anyDoorKeyIsBeingHeld) {
			MagnifyingGlassActive = true;
			cursorType = "magnifying_glass";
			TimerAsOfLastClick = metroGnome;
		}
		if (TimerAsOfLastClick > 192) {
			TimerAsOfLastClick = 0;
		}
		// 320200

		if (metroGnome == 1) {
			TimerAsOfLastClick = -10;
			TimerAsOfEffect = -10;
			TimerAsOfAmbience = 50;
		}

		// rock logic

		if (currentScene.equals("outside") && (TimerAsOfLastClick + 2) < metroGnome && mouseX > 164 && mouseY > 416
				&& mouseX < 294 && mouseY < 445 && mouseDown && !rockIsSearched) {
			playWav(rockSFX);
			rockIsSearched = true;
			TimerAsOfLastClick = metroGnome;
			tooltip = "Who could have guessed?";
		}

		// door logic

		if (currentScene.equals("outside") && (TimerAsOfLastClick + 2) < metroGnome && mouseX > 381 && mouseY > 106
				&& mouseX < 586 && mouseY < 310 && mouseDown && !frontDoorOpen && !anyDoorKeyIsBeingHeld) {
			playWav(knockSFX);
			TimerAsOfLastClick = metroGnome;
			tooltip = "No answer.";
		} else if (currentScene.equals("outside") && (TimerAsOfLastClick + 2) < metroGnome && mouseX > 381
				&& mouseY > 106 && mouseX < 586 && mouseY < 310 && mouseDown && !frontDoorOpen
				&& anyDoorKeyIsBeingHeld) {
			TimerAsOfLastClick = metroGnome;
			tooltip = "It's wide open.";
			frontDoorOpen = true;
			anyDoorKeyIsBeingHeld = false;
			cursorType = "clicker";
			playWav(doorSFX);
		}
		// 208432

		// pick up key logic!!!

		if (currentScene.equals("outside") && (TimerAsOfLastClick + 3) < metroGnome && mouseX > 208 && mouseY > 432
				&& mouseX < 260 && mouseY < 490 && mouseDown && rockIsSearched && !frontDoorKeyCollected) {
			playWav(collectSFX);
			TimerAsOfLastClick = metroGnome;
			anyDoorKeyIsBeingHeld = true;
			frontDoorKeyCollected = true;
			cursorType = "held_key";
			MagnifyingGlassActive = false;
			tooltip = "The key isn't rusted at all.";
		}

		// doorbell ringin' logic

		if (currentScene.equals("outside") && (TimerAsOfLastClick + 10) < metroGnome && mouseX > 320 && mouseY > 200
				&& mouseX < 360 && mouseY < 300 && mouseDown) {
			playWav(doorbellSFX);
			TimerAsOfLastClick = metroGnome;
			tooltip = "The doorbell serves no purpose";
			doorBellIsPressed = true;
		}
		if (TimerAsOfLastClick + 10 == metroGnome) {
			doorBellIsPressed = false;
		}

		// the light switch in the painting room
		if (mouseDown && mouseX > 70 && mouseY > 310 && mouseX < 104 && mouseY < 380 && currentScene.equals("darkroom")
				&& (TimerAsOfLastClick + 10) < metroGnome) {
			currentScene = "lightroom";
			playWav(clickSFX);
			areTheLightsOnInThePaintingRoom = true;
			tooltip = "It's an ugly room.";
		}

		// go-to arrows on the logic side

		if (mouseDown && mouseX > 94 && mouseY > 367 && mouseX < 154 && mouseY < 406 && currentScene.equals("atrium")
				&& (TimerAsOfLastClick + 10) < metroGnome) {
			TimerAsOfLastClick = metroGnome;
			if (areTheLightsOnInThePaintingRoom) {
				currentScene = "lightroom";
				playWav(doorSFX);
				tooltip = "It's an ugly room.";

			} else {
				TimerAsOfLastClick = metroGnome;
				currentScene = "darkroom";
				playWav(doorSFX);
				tooltip = "It's a dark room.";
			}

		}

		if (mouseDown && mouseX > 460 && mouseY > 270 && mouseX < 500 && mouseY < 310 && currentScene.equals("outside")
				&& (TimerAsOfLastClick + 1) < metroGnome && frontDoorOpen) {
			TimerAsOfLastClick = metroGnome;

			currentScene = "hallway";
			playWav(omenSFX);
			tooltip = "It's the hallway.";

		}

		// !!! NOTE:
		// mouse cursor position check is duplicated to leave ZERO logic code in the
		// draw method.

		if (mouseX > 617 && mouseY > 324 && mouseX < 670 && mouseY < 380 & mouseDown
				&& (TimerAsOfLastClick + 10) < metroGnome) {
			currentScene = "atrium";
			TimerAsOfLastClick = metroGnome;
			playWav(doorSFX);
		}

		if (mouseDown && mouseX > 160 && mouseY > 230 && mouseX < 650 && mouseY < 340
				&& (currentScene.equals("darkroom") || currentScene.equals("lightroom"))
				&& cursorType.equals("magnifying_glass")) {
			tooltip = "It's raining outside.";
		} else if (mouseDown && mouseX > 60 && mouseY > 320 && mouseX < 100 && mouseY < 380
				&& currentScene.equals("lightroom") && cursorType.equals("magnifying_glass")) {
			tooltip = "It's a light switch.";
		} else if (mouseDown && mouseX > 670 && mouseY > 300 && mouseX < 700 && mouseY < 430
				&& (currentScene.equals("darkroom") || currentScene.equals("lightroom"))
				&& cursorType.equals("magnifying_glass")) {
			tooltip = "It's the door to the hallway.";
		} else if (mouseDown && (currentScene.equals("darkroom")) && cursorType.equals("magnifying_glass")) {
			tooltip = "It's a room.";
		} else if (mouseDown && (currentScene.equals("lightroom")) && cursorType.equals("magnifying_glass")) {
			tooltip = "It's an ugly room.";
		}

		if (metroGnome == 110 && (TimerAsOfAmbience < metroGnome)) {
			playWav(rainAMB);
			TimerAsOfAmbience = metroGnome + 50;
		}
		if ((metroGnome % 28) == 5 && LatestRNG > 0.5 && thundered == false) {
			playWav(thunderSFX);
			TimerAsOfEffect = metroGnome + 3;
			thundered = true;
		}
		if ((metroGnome % 28) == 7 && LatestRNG > 0.5 && thundered == true) {

			thundered = false;
		}
		if (currentScene.equals("atrium") && mouseX > 500 && mouseY > 180 && mouseX < 530 && mouseY < 217 && mouseDown
				&& MagnifyingGlassActive) {
			tooltip = "It's a pin pointed to the island of Vitruvia.";
		} else if (currentScene.equals("atrium") && mouseX > 500 && mouseY > 180 && mouseX < 530 && mouseY < 217
				&& mouseDown) {
			pinCollected = true;
			tooltip = "It's a pin pointed to the island of Vitruvia.";
		}

	}

	/**
	 * Called every frame. Draw everything here. The cursor is drawn automatically
	 * on top.
	 */
	private void draw(Graphics2D g) {
		g.setFont(TimesNewHamster);
		g.setColor(Color.BLACK);

		g.fillRect(0, 0, WIDTH, HEIGHT);
		switch (currentScene) {
		case "darkroom":
			g.drawImage(room1, 0, 0, WIDTH, HEIGHT, null);
			break;
		case "lightroom":
			g.drawImage(room2, 0, 0, WIDTH, HEIGHT, null);
			break;
		case "atrium":
			g.drawImage(room3, 0, -75, WIDTH, HEIGHT, null);
			if (mouseX > 94 && mouseY > 367 && mouseX < 154 && mouseY < 406) {
				g.drawImage(goto_1, 157, 353, -64, 64, null);
			} else if (currentScene.equals("atrium")) {
				g.drawImage(goto_0, 157, 353, -64, 64, null);
			}
			break;
		// 208432
		case "hallway":
			g.drawImage(room4, 0, 0, WIDTH, HEIGHT, null);
			break;
		case "outside":
			g.drawImage(outside0, 0, 0, WIDTH, HEIGHT, null);
			if ((TimerAsOfEffect + 1) > metroGnome && currentScene.equals("outside") && !frontDoorOpen) {
				g.drawImage(outside1, 0, 0, WIDTH, HEIGHT, null);
			}
			if (frontDoorOpen) {
				g.drawImage(outside2, 0, 0, WIDTH, HEIGHT, null);
				if (mouseX > 460 && mouseY > 270 && mouseX < 500 && mouseY < 310) {
					g.drawImage(getRotatedImage(goto_1, -90), 516, 244, -64, 64, null);
				} else {
					g.drawImage(getRotatedImage(goto_0, -90), 516, 244, -64, 64, null);
				}
			}
			if (!rockIsSearched) {
				g.drawImage(rock0, 0, 0, WIDTH, HEIGHT, null);
			} else if (rockIsSearched && !frontDoorKeyCollected) {
				g.drawImage(rock1, 0, 0, WIDTH, HEIGHT, null);
				g.drawImage(key0, 0, 0, WIDTH, HEIGHT, null);

			} else {
				g.drawImage(rock1, 0, 0, WIDTH, HEIGHT, null);

			}

			break;
		default:
			break;
		}

		if (doorBellIsPressed) {
			g.drawImage(overlay_doorbell, 0, 0, WIDTH, HEIGHT, null);
		}
		// 93 353
		// ── TODO: your drawing code ───────────────────────────────────────────
		g.setColor(Color.WHITE);
		if (debugMode) {
			g.setColor(Color.PINK);
			g.drawString("DEBUGGING", 20, 30);
			g.drawString("MouseX " + Integer.toString(mouseX), 20, 60);
			g.drawString("MouseY " + Integer.toString(mouseY), 20, 90);
			g.drawString("Game timer: " + Integer.toString(metroGnome), 20, 120);

			g.drawString("Mouse is being clicked: " + Boolean.toString(mouseDown), 20, 150);
			g.drawString("Magnifying Glass Active: " + Boolean.toString(MagnifyingGlassActive), 20, 180);
			g.drawString("Hungy? " + Boolean.toString(foundSomething), 20, 210);
			g.drawString("Last Click: " + Integer.toString(TimerAsOfLastClick), 20, 240);
			g.drawString("Scene: " + currentScene, 20, 270);
			g.drawString("TAOA: " + Integer.toString(TimerAsOfAmbience), 20, 300);
		}
		g.setColor(Color.WHITE);
//        (Math.random() * 5) +
		if ((TimerAsOfEffect + 1) > metroGnome && currentScene.equals("darkroom")) {
			g.drawImage(room1_lightning, 0, 0, WIDTH, HEIGHT, null);
		} else if ((TimerAsOfEffect + 1) > metroGnome && currentScene.equals("atrium")) {
			g.drawImage(room3_lightning, 0, -75, WIDTH, HEIGHT, null);
		}

		// go-to arrows
		if (mouseX > 617 && mouseY > 324 && mouseX < 670 && mouseY < 380
				&& (currentScene.equals("lightroom") || currentScene.equals("darkroom"))) {
			g.drawImage(goto_1, 617, 324, 64, 64, null);
		} else if (currentScene.equals("lightroom") || currentScene.equals("darkroom")) {
			g.drawImage(goto_0, 617, 324, 64, 64, null);
		}

		g.drawImage(bgImage, 0, 0, WIDTH, HEIGHT, null);
		g.setColor(Color.WHITE);

		g.drawString("Some Thoughts:", 8, 560);
		g.setFont(TimesNewHamsterPro);
		g.drawString(tooltip, 8, 590);

		g.setFont(TimesNewHamster);
		if (!mouseDown && mouseX > 270 && mouseY > 450 && mouseX < 430 && mouseY < 580) {
			g.drawImage(uiButton0, 270, 450, 160, 160, null);
			foundSomething = true;
		} else if (mouseDown && mouseX > 270 && mouseY > 450 && mouseX < 430 && mouseY < 580) {
			g.drawImage(uiButton1, 270, 450, 160, 160, null);
			foundSomething = true;

		} else {
			g.drawImage(uiButton1, 270, 450, 160, 160, null);
		}
		if (!MagnifyingGlassActive) {
			g.drawImage(magnifying_glass, 270, 450, 160, 160, null);
		}

		if (!mouseDown && mouseX > 470 && mouseY > 450 && mouseX < 630 && mouseY < 580) {
			g.drawImage(uiButton0, 470, 450, 160, 160, null);
			foundSomething = true;
		} else {
			g.drawImage(uiButton1, 470, 450, 160, 160, null);
		}
		if ((!(mouseX > 470 && mouseY > 450 && mouseX < 630 && mouseY < 580)
				&& !(mouseX > 270 && mouseY > 450 && mouseX < 430 && mouseY < 580))) {
			foundSomething = false;
		}
		if (!pinCollected && currentScene.equals("atrium")) {
			g.drawImage(pin, 0, -75, WIDTH, HEIGHT, null);
		}
	}
// 617 324
	// =========================================================================
	// CURSOR — edit drawCursor() to swap the red dot for an image
	// =========================================================================

	/**
	 * Draws the custom cursor at the current mouse position.
	 *
	 * TO USE AN IMAGE INSTEAD OF THE RED DOT: 1. Load it once in setup():
	 * cursorImage = ImageIO.read(new File("cursor.png")); 2. In drawCursor(),
	 * replace the fillOval block with: g.drawImage(cursorImage, mouseX - hotspotX,
	 * mouseY - hotspotY, null); where hotspotX/hotspotY are the pixel within the
	 * image that acts as the tip.
	 */
	private void drawCursor(Graphics2D g) {

		// ── Option A: red dot (default) ───────────────────────────────────────
//        int r = 8;
//        g.setColor(Color.RED);
//        g.fillOval(mouseX - r, mouseY - r, r * 2, r * 2);
		int hotspotX = 0, hotspotY = 0; // which pixel in the image is the tip
		if (cursorType.equals("clicker")) {
			if (foundSomething) {
				g.drawImage(cursor1, mouseX - hotspotX, mouseY - hotspotY, 64, 64, null);
			} else if (mouseDown) {
				g.drawImage(cursor2, mouseX - hotspotX, mouseY - hotspotY, 64, 64, null);
			} else {
				g.drawImage(cursor0, mouseX - hotspotX, mouseY - hotspotY, 64, 64, null);
			}
		}

		if (cursorType.equals("magnifying_glass")) {
//    		if (mouseDown) {
			g.drawImage(magnifying_cursor, mouseX - hotspotX, mouseY - hotspotY, 64, 64, null);
//    		} else {
//    			g.drawImage(cursor0, mouseX - hotspotX, mouseY - hotspotY, 40, 40, null);
//    		}
		}

		if (cursorType.equals("held_key")) {
			g.drawImage(key_cursor, mouseX - hotspotX, mouseY - hotspotY, 64, 64, null);
		}

	}

	// ── Uncomment this field when you switch to an image cursor ──────────────
	// private java.awt.image.BufferedImage cursorImage;

	// =========================================================================
	// ENTRY POINT
	// =========================================================================
	public static void main(String[] args) {

		Timer metronome = new Timer(500, e -> {

			metroGnome = (metroGnome + 1) % 201;
		});
		metronome.start();
		SwingUtilities.invokeLater(() -> {
			GameWindow gw = new GameWindow();
			gw.setVisible(true);
			new Thread(() -> {
				try {
					try {
						gw.run();
					} catch (LineUnavailableException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (UnsupportedAudioFileException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				} catch (IOException e) {
					System.out.println("oops");
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}).start();
		});
	}
}
