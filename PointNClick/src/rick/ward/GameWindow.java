package rick.ward;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Random;

public class GameWindow extends JFrame {

	public static double GameVersion = 0.1;

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

	private Clip rain;
	private Clip jazz;

	public void jazzAmbience(File file) {

		// Run in a background thread to keep your GUI responsive
		new Thread(() -> {
			try {
				AudioInputStream inputStream = AudioSystem.getAudioInputStream(file);

				// Synchronized ensures stop() and play() don't collide
				synchronized (this) {
					jazz = AudioSystem.getClip();
					jazz.open(inputStream);
				}

				// This tells Java to loop the clip indefinitely at the hardware/driver level
				jazz.loop(Clip.LOOP_CONTINUOUSLY);

				// Clean up memory when done
				jazz.addLineListener(event -> {
					if (event.getType() == LineEvent.Type.STOP) {
						synchronized (this) {
							if (rain != null) {
								rain.close();
							}
						}
					}
				});

			} catch (Exception e) {
				System.err.println("Playback error: " + e.getMessage());
			}
		}).start();
	}

	public void rainAmbience(File file) {

		// Run in a background thread to keep your GUI responsive
		new Thread(() -> {
			try {
				AudioInputStream inputStream = AudioSystem.getAudioInputStream(file);

				// Synchronized ensures stop() and play() don't collide
				synchronized (this) {
					rain = AudioSystem.getClip();
					rain.open(inputStream);
				}

				// This tells Java to loop the clip indefinitely at the hardware/driver level
				rain.loop(Clip.LOOP_CONTINUOUSLY);

				// Clean up memory when done
				rain.addLineListener(event -> {
					if (event.getType() == LineEvent.Type.STOP) {
						synchronized (this) {
							if (rain != null) {
								rain.close();
							}
						}
					}
				});

			} catch (Exception e) {
				System.err.println("Playback error: " + e.getMessage());
			}
		}).start();
	}

	public synchronized void setRainVolume(float volume) {
		if (rain == null)
			return;

		// Ensure the volume bounds stay between 0.0 and 1.0
		volume = Math.max(0.0f, Math.min(1.0f, volume));

		try {
			// Get the Master Gain control (controls decibels)
			FloatControl gainControl = (FloatControl) rain.getControl(FloatControl.Type.MASTER_GAIN);

			// Logarithmic volume scaling (linear percentages don't sound right to human
			// ears)
			float dB = (float) (Math.log10(volume) * 20.0);

			// Handle absolute silence safely
			if (volume == 0.0f) {
				dB = gainControl.getMinimum();
			}

			gainControl.setValue(dB);
		} catch (IllegalArgumentException e) {
			System.err.println("Volume control not supported by this audio format.");
		}
	}

	/**
	 * Stops the currently playing WAV file immediately.
	 */
	public synchronized void stopJazz() {
		if (jazz != null && jazz.isRunning()) {
			jazz.stop();
			jazz.close();
		}
	}
	// =================
	// UNTIL THIS POINT
	// =================

	/*
	 * Method Name: writeOut Author: Kyle McKay Creation Date; Nov 15 2023 Modified
	 * Date: Nov 15 2023 Description: Creates a file of the integer array
	 * 
	 * @Parameters: A integer array and the file name as a string.
	 * 
	 * @Return Value: None its a procedure Data Type: integer ARRAY Dependencies:
	 * n/a Throws/Exceptions: File IO exceptions
	 */
	public static void writeOut(String filename, String[] Array) {
		try {
			PrintWriter outputfile = new PrintWriter(new BufferedWriter(new FileWriter(filename)));

			for (int i = 0; i < Array.length; i++) {
				outputfile.println(Array[i]);
			}

			outputfile.close();

		} catch (Exception e) {
			System.out.println("E-RR0R: " + e.toString());
		}

	}

	private boolean mouseDown = false; // updated to true upon mouse beginning to be pressed and false when unpressed.
										// works with left, middle and right click and afaik extra buttons on gaming
										// mice.

	private boolean inMenu = true; // used to indicate if certain ui elements should be functional or not. since,
									// yknow, they shouldnt work in the menu (or be drawn)
	private boolean PlayButtonHover = false; // used to indicate if the button is gray
	private boolean LoadButtonHover = false;// used to indicate if the button is gray

	private boolean didThePlayerJustPressS = false; // used to know if the player has pressed S to enter the save
													// selector

	private String cursorType = "menu"; // the player's cursortype. can be a key, the black blob in the menu, the
										// magnifying glass, or the normal cursor
	private String currentKeyColour = "spoink"; // SPOINK IS A VALUE USED TO INDICATE THERE IS NO KEY IN THE PLAYERS
												// INVENTORY
	private boolean foundSomething = false; // used for the mouse cursor having a white highlight when hovering over the
											// magnifying glass icon!
	private boolean PaintingMoved = false; // the painting in the room with the light switch!!!

	private boolean purpleKeyCollected = false;
	private boolean orangeKeyCollected = false; // bunch o' gamestate variables to ensure there arent infinite places to
												// get keys on the map LOL
	private boolean greenKeyCollected = false;
	public String gameName; // filled with a random funny name on Setup()

	private boolean debugMode = false; // change to false before submitting
	// ^^^ gives the player cheats if enabled with I

	private int GameTimerWhenButtonsAppear = 5; // my favourite coding convention: magic numbers
	private boolean DrawTheButtons = false; // used for cinematic delay on the menu

	private int GameTimerWhenPreambleAppears = 9; // my favourite coding convention: magic numbers
	private boolean DrawThePreamble = false; // used for cinematic delay on the menu

	private int GameTimerWhenSignatureAppears = 12; // my favourite coding convention: magic numbers
	private boolean DrawTheSignature = false; // used for cinematic delay on the menu

	private boolean pinCollected = false; // this is a misnomer. it represents the key stuck into the globe in the
											// atrium
	private boolean anyDoorKeyIsBeingHeld = false;
	private boolean frontDoorKeyCollected = false; // self-explanatory
	private boolean doorBellIsPressed = false; // used for the dark gray colour the doorbell has when rung
	private boolean rockIsSearched = false; // did they move the rock to find the key yet?
	private boolean frontDoorOpen = false; // irelevant after the player enters the house.
	private int numLocksRemaining = 3; // how many locks does the player have left to find the keys for?
	private boolean theBasementDoorIsOpenYouCanCompleteTheGame = false; // i love longly-named variables

	private boolean areTheLightsOnInThePaintingRoom = false; // one of the so-called gamestate variables.

	private Font TimesNewHamster = new Font("Dialog", Font.BOLD, 10); // A small FONT
	private Font TimesNewHamsterPro = new Font("Dialog", Font.BOLD, 15); // a BIG font

	private int TimerAsOfLastClick = 0; // used to prevent clickable objects from activating every frame.
	private int TimerAsOfEffect = 0; // used to make sure lightning does not happen too close together
	private int TimerAsOfAmbience = 0; // unused, but i am afraid to remove it.

	private String tooltip = "This is the address."; // displayed in the bottom left corner of the screen. adds humour

	// ALL SOUNDS
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
	private static File musicAMB = new File("finish_game.wav");
	private static File jazzAMB = new File("Kevin Macleod - Vibing Over Venus.wav");

	private static double LatestRNG = 0; // used for any RNG element in the game. unfortunately, it is only used for the
											// lightning.

	private static int metroGnome = 0; // udpated every half-second and loops when at 200. used to time animations.

	private static String currentScene = "menu"; // what is the current "room" that the player is in. HEAVILY important.
													// used for drawing and updating.

	private static boolean rainStarted = false; // is it raining? basically, is the cinematic delay over

	private boolean MagnifyingGlassActive = false;
	private boolean MagnifyingGlassHover = false;

	private int lastInputtedSaveNumber = -1; // used as a layer of nuance in the file saver

	private BufferedImage bgImage;
	private BufferedImage uiButton0;
	private BufferedImage uiButton1;

	private BufferedImage playButton0;
	private BufferedImage playButton1;

	private BufferedImage loadButton0;
	private BufferedImage loadButton1;

	private BufferedImage goto_0;
	private BufferedImage goto_1;

	private BufferedImage overlay_doorbell;

	private BufferedImage menu;

	private boolean menu_LoadSavePressed = false; // did the player just press the load save button????

	private BufferedImage room1;
	private BufferedImage room1_lightning;
	private BufferedImage room2;
	private BufferedImage room3;
	private BufferedImage room3_lightning;
	private BufferedImage room4;
	private BufferedImage room5;
	private BufferedImage outside0;
	private BufferedImage outside1;
	private BufferedImage outside2;

	private BufferedImage rock0;
	private BufferedImage rock1;
	private BufferedImage key0;
	private BufferedImage b_door_3;
	private BufferedImage b_door_2;
	private BufferedImage b_door_1;
	private BufferedImage b_door_0;
	private BufferedImage b_door_open;

	private BufferedImage black;

	private BufferedImage credit;

	private BufferedImage signature;

	private BufferedImage cursor0;
	private BufferedImage cursor1;
	private BufferedImage cursor2;
	private BufferedImage cursor3;

	private BufferedImage magnifying_cursor;
	private BufferedImage key_cursor;

	private BufferedImage magnifying_glass;
	private BufferedImage diary;

	private BufferedImage pin;
	private BufferedImage key_pink;
	private BufferedImage key_orange;
	private BufferedImage key_green;

	private String[] SaveArray = new String[10]; // used as a temp when saving a gamestate.
	private String[] SaveArrayIn = new String[10]; // used as a temp when loading a gamestate

	/*
	 * Method Name: readIn Author: Kyle McKay Creation Date; Nov 15 2023 Modified
	 * Date: Nov 15 2023 Description: Reads line by line the integers in a file
	 * places in an array
	 * 
	 * @Parameters: A integer array, and file name as a string
	 * 
	 * @Return Value: Returns the filled in array Data Type: integer ARRAY
	 * Dependencies: n/a Throws/Exceptions: File IO exceptions
	 */
	public static String[] readIn(String filename, String[] Array) {
		String dataItem;
		try {
			BufferedReader FileInputPointer = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
			int i = 0; // index of the array
			while (FileInputPointer.ready() == true) {
				dataItem = FileInputPointer.readLine();
//				System.out.println("Yo, I read in this, yo:" + dataItem);
				Array[i] = dataItem;
				i++;
			}
			FileInputPointer.close();
		} catch (FileNotFoundException e) {
			System.out.println("YOU'RE TRYING TO PASS OFF SOME GARBAGE!");
		} catch (IOException e) {
			System.out.println("E-RR0R" + e.toString());
		}
		return Array;
	}

	private BufferedImage cutout;
	private BufferedImage painting;

	private static int cinematicDelay = 999; // in leaveMenu, this is the number of halfseconds that the screen will be
												// black upon entering a new game.
	private static boolean cinematicDelayOver = false; // used to make sure that on the metroGnome tick when they are
														// initally different, the things that happen at the end of the
														// delay do not loop infinitively.

	/**
	 * Author: Richard Does: It is a collection of game variables set once the
	 * player leaves the menu.
	 */
	private void leaveMenu() {
		if (metroGnome > 190) {
			cinematicDelay = metroGnome - 190;
		} else {
			cinematicDelay = metroGnome + 10;
		}
		currentScene = "blank";
		cursorType = "clicker";
		inMenu = false;
		stopJazz();
		playWav(omenSFX);
	}

	/**
	 * Author: R. W. Created: 16 Jun 2026 Does: it's a modification of LeaveMenu
	 * that removes the cinematic! Type: Void
	 */
	private void loadFromMenu() {
		if (metroGnome > 190) {
			cinematicDelay = metroGnome - 190;
		} else {
			cinematicDelay = metroGnome + 10;
		}
		cursorType = "clicker";
		inMenu = false;
		cinematicDelayOver = true;
		stopJazz();
		playWav(omenSFX);
	}

	/**
	 * Author: Someone named Rick Created: 16 Jun 2026 DOES: IT SETS AN ARRAY TO ALL
	 * MAJOR GAME VARIABlES!!!
	 * 
	 * @param String[]
	 */
	private void GenSaveArray(String[] Hamster) {
		Hamster[0] = Boolean.toString(PaintingMoved);
		Hamster[1] = currentScene;
		Hamster[2] = Boolean.toString(theBasementDoorIsOpenYouCanCompleteTheGame);
		Hamster[3] = Boolean.toString(pinCollected);
		Hamster[4] = Integer.toString(numLocksRemaining);

		Hamster[5] = Boolean.toString(greenKeyCollected);
		Hamster[6] = Boolean.toString(purpleKeyCollected);
		Hamster[7] = Boolean.toString(orangeKeyCollected);

		Hamster[8] = currentKeyColour;
		Hamster[9] = Boolean.toString(areTheLightsOnInThePaintingRoom);
	}

	/**
	 * Author: Someone named Rick DOES: IT SETS ALL MAJOR GAME VARIABLES TO WHAT WAS
	 * READ IN FROM THE ARRAY THAT THE USER CHOSE!!! Created: 16 Jun 2026
	 * 
	 * @param String[]
	 */
	private void ReverseGenSaveArray(String[] Hamster) {
		PaintingMoved = Boolean.parseBoolean(Hamster[0]);
		currentScene = Hamster[1];
		theBasementDoorIsOpenYouCanCompleteTheGame = Boolean.parseBoolean(Hamster[2]);
		pinCollected = Boolean.parseBoolean(Hamster[3]);
		numLocksRemaining = Integer.parseInt(Hamster[4]);

		greenKeyCollected = Boolean.parseBoolean(Hamster[5]);
		purpleKeyCollected = Boolean.parseBoolean(Hamster[6]);
		orangeKeyCollected = Boolean.parseBoolean(Hamster[7]);

		currentKeyColour = Hamster[8];
		areTheLightsOnInThePaintingRoom = Boolean.parseBoolean(Hamster[9]);
	}

	// -------------------------------------------------------------------------
	// Config
	// -------------------------------------------------------------------------
	public static final int WIDTH = 800; // width of the gameWindow. dont change it.
	public static final int HEIGHT = 600; // height of the gameWindow. dont change it.
	private static final int TARGET_FPS = 60; // fps of the gameWindow. dont change it.

	private int mouseX = 0; // updated in Update() and is the current X position of the mouse relative to
							// the top left corner of the window
	private int mouseY = 0; // updated in Update() and is the current Y position of the mouse relative to
							// the top left corner of the window

	private final Canvas canvas = new Canvas() {
		@Override
		public void paint(Graphics g) {

		}
	};

	public GameWindow() {
		canvas.setFocusable(true);
		canvas.requestFocus();
		canvas.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				int key = e.getKeyCode();
				if (key == KeyEvent.VK_1) {
					if (menu_LoadSavePressed) {
						menu_LoadSavePressed = false;
						readIn("savefiles/1.txt", SaveArrayIn);
						ReverseGenSaveArray(SaveArrayIn);
						loadFromMenu();
					}
					if (didThePlayerJustPressS && !inMenu) {
						didThePlayerJustPressS = false;
						lastInputtedSaveNumber = 1;
						String SaveFileName = "savefiles/" + Integer.toString(lastInputtedSaveNumber) + ".txt";
						GenSaveArray(SaveArray);
						writeOut(SaveFileName, SaveArray);
					}
				} else if (key == KeyEvent.VK_S) {
					didThePlayerJustPressS = true;
				}
				if (key == KeyEvent.VK_2) {
					if (menu_LoadSavePressed) {
						menu_LoadSavePressed = false;
						readIn("savefiles/2.txt", SaveArrayIn);
						ReverseGenSaveArray(SaveArrayIn);
						loadFromMenu();
					}
					if (didThePlayerJustPressS && !inMenu) {
						didThePlayerJustPressS = false;
						lastInputtedSaveNumber = 2;
						String SaveFileName = "savefiles/" + Integer.toString(lastInputtedSaveNumber) + ".txt";
						GenSaveArray(SaveArray);
						writeOut(SaveFileName, SaveArray);
					}
				} else if (key == KeyEvent.VK_3) {
					if (menu_LoadSavePressed) {
						menu_LoadSavePressed = false;
						readIn("savefiles/3.txt", SaveArrayIn);
						ReverseGenSaveArray(SaveArrayIn);
						loadFromMenu();
					}
					if (didThePlayerJustPressS && !inMenu) {
						didThePlayerJustPressS = false;
						lastInputtedSaveNumber = 3;
						String SaveFileName = "savefiles/" + Integer.toString(lastInputtedSaveNumber) + ".txt";
						GenSaveArray(SaveArray);
						writeOut(SaveFileName, SaveArray);
					} else {

					}
				} else if (key == KeyEvent.VK_4) {
					currentScene = "Main Hall";
				} else if (key == KeyEvent.VK_5) {
					currentScene = "Courtyard";
				} else if (key == KeyEvent.VK_6) {
					currentScene = "Foyer";
				} else if (key == KeyEvent.VK_7) {
					currentScene = "Atrium";
				} else if (key == KeyEvent.VK_8) {
					currentScene = "A dark void";
				} else if (key == KeyEvent.VK_9) {
					currentScene = "Painting Room";

				} else if (key == KeyEvent.VK_M) {
					currentScene = "menu";
					inMenu = true;
				} else if (key == KeyEvent.VK_I) {
					if (debugMode) {
						debugMode = false;
					} else if (!debugMode) {
						debugMode = true;
					}
				} else if (key == KeyEvent.VK_C) {
					if (debugMode) {
						if (theBasementDoorIsOpenYouCanCompleteTheGame) {
							theBasementDoorIsOpenYouCanCompleteTheGame = false;
						} else {
							theBasementDoorIsOpenYouCanCompleteTheGame = true;
						}
					}
				} else if (key == KeyEvent.VK_V) {

					if (debugMode)
						if (numLocksRemaining >= 0 && numLocksRemaining < 3) {
							numLocksRemaining++;
						} else {
							numLocksRemaining = 0;
						}

				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				// fired when key is let go (duh)
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
		canvas.setIgnoreRepaint(true);
		add(canvas);
		pack();
		setLocationRelativeTo(null);

		Toolkit tk = Toolkit.getDefaultToolkit();
		BufferedImage blank = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Cursor hiddenCursor = tk.createCustomCursor(blank, new Point(0, 0), "hidden");
		canvas.setCursor(hiddenCursor);

	}

	// =========================================================================
	// MAIN LOOP
	// =========================================================================
	public static void playWav(File file) {
		new Thread(() -> {
			try {
				AudioInputStream inputStream = AudioSystem.getAudioInputStream(file);
				Clip clip = AudioSystem.getClip();
				clip.open(inputStream);
				clip.start();

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

	/**
	 * Author: R. A. Ward Does: Sets up the gameWindow thread for use in useful
	 * scenarios. Createdated: 16-Jun-2026
	 * 
	 * @throws IOException
	 * @throws LineUnavailableException
	 * @throws UnsupportedAudioFileException
	 */
	private void run() throws IOException, LineUnavailableException, UnsupportedAudioFileException {
		setVisible(true);
		canvas.createBufferStrategy(2);

		final long nsPerFrame = 1_000_000_000L / TARGET_FPS;

		setup();

		while (true) {
			long frameStart = System.nanoTime();

			update();

			do {
				Graphics2D g = (Graphics2D) canvas.getBufferStrategy().getDrawGraphics();
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				draw(g);
				drawCursor(g);

				g.dispose();
			} while (canvas.getBufferStrategy().contentsRestored());
			canvas.getBufferStrategy().show();

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

	/**
	 * Author: R. A. Ward Does: Sets up the gameWindow's variables, and starts music
	 * for use in party scenarios. Createdated: 16-Jun-2026
	 */
	private void setup() throws IOException {
		String[] Titles = { "Proffesor", "Doctor", "Inspector", "Detective", "Mr.", "Grand Inquisitor", "Dr.", "Sir",
				"Lord" };
		String[] ProperNouns = { "Blambry", "Cave Johnson", "Steve", "Einstein", "Edlai", "Virgil", "Wheatley", "Nigel",
				"Stirling", "Conly", "Greg" };
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
		jazzAmbience(jazzAMB);
		menu = ImageIO.read(new File("title_screen.png"));
		bgImage = ImageIO.read(new File("ui_scale4.png"));
		uiButton0 = ImageIO.read(new File("button_scale4_0.png"));
		uiButton1 = ImageIO.read(new File("button_scale4_1.png"));
		b_door_3 = ImageIO.read(new File("door_3_locks.png"));
		b_door_2 = ImageIO.read(new File("door_2_locks.png"));
		;
		b_door_1 = ImageIO.read(new File("door_1_lock.png"));
		;
		b_door_0 = ImageIO.read(new File("door_no_locks.png"));
		;
		b_door_open = ImageIO.read(new File("ominous_door_open.png"));
		;

		black = ImageIO.read(new File("void.png"));

		playButton0 = ImageIO.read(new File("playbutton0.png"));
		playButton1 = ImageIO.read(new File("playbutton1.png"));

		loadButton0 = ImageIO.read(new File("loadbutton0.png"));
		loadButton1 = ImageIO.read(new File("loadbutton1.png"));

		signature = ImageIO.read(new File("signature.png"));
		credit = ImageIO.read(new File("credit_prelude.png"));

		room1 = ImageIO.read(new File("darkroom_0_scale4.png"));
		room1_lightning = ImageIO.read(new File("darkroom_1_scale4.png"));
		room2 = ImageIO.read(new File("lightroom_scale4.png"));
		room3 = ImageIO.read(new File("room3.png"));

		outside0 = ImageIO.read(new File("front_yard_no_lightning.png"));
		outside1 = ImageIO.read(new File("front_yard_yes_lightning.png"));
		outside2 = ImageIO.read(new File("doors_open.png"));

		key_cursor = ImageIO.read(new File("key_cursor.png"));
		cursor3 = ImageIO.read(new File("menu_cursor.png"));

		overlay_doorbell = ImageIO.read(new File("doorbell_pressed_overlay.png"));

		rock0 = ImageIO.read(new File("rock0.png"));
		rock1 = ImageIO.read(new File("rock1.png"));
		;
		key0 = ImageIO.read(new File("key_onGround.png"));
		room5 = ImageIO.read(new File("room_foyer.png"));

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
		key_orange = ImageIO.read(new File("key_orange.png"));
		key_pink = ImageIO.read(new File("key_pink.png"));
		key_green = ImageIO.read(new File("key_green.png"));
		painting = ImageIO.read(new File("painting.png"));
		cutout = ImageIO.read(new File("cutout.png"));
	}

	private boolean thundered = false; // used to make sure that the thunder sound doesn't start every frame while the
										// random thunder occurencing is happening
	private int SpecialDoorbellDelay = 0; // used to give the doorbell an EXTRA long click delay. it does not use
											// TimerAsOfLastClick

	/**
	 * Author: R. A. Ward Does: Updates the Game Logic. called continuosly while the
	 * game is being run. VERY important. Createdated: 16-Jun-2026
	 * 
	 * @throws LineUnavailableException
	 * @throws UnsupportedAudioFileException
	 */
	private void update() throws LineUnavailableException, UnsupportedAudioFileException {
		LatestRNG = Math.random();

		PointerInfo pi = MouseInfo.getPointerInfo();
		Point p = pi.getLocation();
		SwingUtilities.convertPointFromScreen(p, canvas);
		mouseX = p.x;
		mouseY = p.y;

		// MENU GNOME!!!!!!!!!
		if (inMenu) {
			if (GameTimerWhenButtonsAppear < metroGnome) {
				DrawTheButtons = true;
			}
			if (GameTimerWhenPreambleAppears < metroGnome) {
				DrawThePreamble = true;
			}
			if (GameTimerWhenSignatureAppears < metroGnome) {
				DrawTheSignature = true;
			}
			if (mouseX > 44 && mouseY > 358 && mouseX < 338 && mouseY < 496) {
				PlayButtonHover = true;
				if (mouseDown) {
					leaveMenu();
				}
			} else {
				PlayButtonHover = false;
			}

			if (mouseX > 467 && mouseY > 358 && mouseX < 740 && mouseY < 496) {
				LoadButtonHover = true;
				if (mouseDown) {
					menu_LoadSavePressed = true;
				}
			} else {
				LoadButtonHover = false;
			}
//
		} else {

			if (!PaintingMoved && currentScene.equals("Painting Room")
					&& (mouseX > 328 && mouseY > 85 && mouseX < 500 && mouseY < 140) && mouseDown
					&& cursorType.equals("clicker") && (TimerAsOfLastClick + 1 < metroGnome)) {
				PaintingMoved = true;
				TimerAsOfLastClick = metroGnome;
				playWav(rockSFX);
				tooltip = "Oldest trick in the book!";
			}
			if (!greenKeyCollected && currentKeyColour.equals("spoink") && PaintingMoved
					&& currentScene.equals("Painting Room")
					&& (mouseX > 447 && mouseY > 115 && mouseX < 500 && mouseY < 179) && mouseDown
					&& cursorType.equals("clicker") && (TimerAsOfLastClick + 1 < metroGnome)) {
				currentKeyColour = "green";
				greenKeyCollected = true;
				tooltip = "How secure.";

			}

			if (theBasementDoorIsOpenYouCanCompleteTheGame && currentScene.equals("Foyer")
					&& (mouseX > 238 && mouseY > 379 && mouseX < 274 && mouseY < 423) && mouseDown) {
				currentScene = "basement";
				playWav(musicAMB);
				setRainVolume(-1.0f);
			}
			if (metroGnome > cinematicDelay && !cinematicDelayOver) {
				cinematicDelayOver = true;
				currentScene = "Courtyard";
				if (!rainStarted) {
					rainAmbience(rainAMB);
					rainStarted = true;
				}
			}

			if (mouseDown && mouseX > 270 && mouseY > 450 && mouseX < 430 && mouseY < 580 && MagnifyingGlassActive
					&& (TimerAsOfLastClick + 1) < metroGnome) {
				MagnifyingGlassActive = false;
				cursorType = "clicker";
				TimerAsOfLastClick = metroGnome;
			} else if (mouseDown && mouseX > 270 && mouseY > 450 && mouseX < 430 && mouseY < 580
					&& !cursorType.equals("held_key") && !MagnifyingGlassActive
					&& (TimerAsOfLastClick + 1) < metroGnome) {
				MagnifyingGlassActive = true;
				cursorType = "magnifying_glass";
				TimerAsOfLastClick = metroGnome;
			}

			if (metroGnome > 196) {
				TimerAsOfLastClick = metroGnome - 196;
				TimerAsOfEffect = metroGnome - 196;
				SpecialDoorbellDelay = metroGnome - 196;

			}

			// the door to the basement begins here
			// 190, 247
			// 309, 450
			// theBasementDoorIsOpenYouCanCompleteTheGame

			if (currentScene.equals("Foyer")) {
				if (!purpleKeyCollected && cursorType.equals("held_key") && mouseX > 190 && mouseY > 247 && mouseX < 309
						&& mouseY < 450 && (TimerAsOfLastClick + 1) < metroGnome && mouseDown
						&& numLocksRemaining > 0) {
					TimerAsOfLastClick = metroGnome;
					numLocksRemaining--;
					currentKeyColour = "spoink";
					cursorType = "clicker";
					playWav(omenSFX);
					tooltip = "It unlocked like a Spoink on a Friday night.";
				} else if (!theBasementDoorIsOpenYouCanCompleteTheGame && numLocksRemaining == 0 && mouseX > 190
						&& mouseY > 247 && mouseX < 309 && mouseY < 450 && (TimerAsOfLastClick + 1) < metroGnome
						&& mouseDown) {
					theBasementDoorIsOpenYouCanCompleteTheGame = true;
					tooltip = "How ominous...";
					playWav(doorSFX);
				} else if (!theBasementDoorIsOpenYouCanCompleteTheGame && mouseX > 190 && mouseY > 247 && mouseX < 309
						&& mouseY < 450 && (TimerAsOfLastClick + 1) < metroGnome && mouseDown) {
					TimerAsOfLastClick = metroGnome;
					playWav(knockSFX);
					tooltip = "No one could possibly be behind this door.";
				}
			}
			// ends here

			// rock logic

			if (currentScene.equals("Courtyard") && (TimerAsOfLastClick + 1) < metroGnome && mouseX > 164
					&& mouseY > 416 && mouseX < 294 && mouseY < 445 && mouseDown && !rockIsSearched) {
				playWav(rockSFX);
				rockIsSearched = true;
				TimerAsOfLastClick = metroGnome;
				tooltip = "Who could have guessed?";
			}

			// door logic

			if (currentScene.equals("Courtyard") && (TimerAsOfLastClick + 1) < metroGnome && mouseX > 381
					&& mouseY > 106 && mouseX < 586 && mouseY < 310 && mouseDown && !frontDoorOpen
					&& !anyDoorKeyIsBeingHeld) {
				playWav(knockSFX);
				TimerAsOfLastClick = metroGnome;
				tooltip = "No answer.";
			} else if (currentScene.equals("Courtyard") && (TimerAsOfLastClick + 1) < metroGnome && mouseX > 381
					&& mouseY > 106 && mouseX < 586 && mouseY < 310 && mouseDown && !frontDoorOpen
					&& anyDoorKeyIsBeingHeld) {
				TimerAsOfLastClick = metroGnome;
				tooltip = "It's wide open.";
				currentKeyColour = "spoink";
				frontDoorOpen = true;
				anyDoorKeyIsBeingHeld = false;
				cursorType = "clicker";
				playWav(doorSFX);
			}
			// 208432

			// pick up key logic!!!

			if (currentScene.equals("Courtyard") && (TimerAsOfLastClick + 1) < metroGnome && mouseX > 208
					&& mouseY > 432 && mouseX < 260 && mouseY < 490 && mouseDown && rockIsSearched
					&& !frontDoorKeyCollected) {

				TimerAsOfLastClick = metroGnome;
				anyDoorKeyIsBeingHeld = true;
				frontDoorKeyCollected = true;
//				cursorType = "held_key";
				MagnifyingGlassActive = false;
				currentKeyColour = "brass";
				tooltip = "The key isn't rusted at all.";
			}

			// doorbell ringin' logic

			if (currentScene.equals("Courtyard") && (SpecialDoorbellDelay + 10) < metroGnome && mouseX > 320
					&& mouseY > 200 && mouseX < 360 && mouseY < 300 && mouseDown) {
				SpecialDoorbellDelay = metroGnome;
				playWav(doorbellSFX);

				tooltip = "The doorbell serves no purpose";
				doorBellIsPressed = true;
			}
			if (SpecialDoorbellDelay + 10 == metroGnome) {
				doorBellIsPressed = false;
			}

			// the light switch in the painting room
			if (mouseDown && mouseX > 70 && mouseY > 310 && mouseX < 104 && mouseY < 380
					&& currentScene.equals("A dark void") && (TimerAsOfLastClick + 1) < metroGnome) {
				currentScene = "Painting Room";
				playWav(clickSFX);
				areTheLightsOnInThePaintingRoom = true;
				tooltip = "It's an ugly room.";
			}

			// go-to arrows on the logic side
			if (mouseX > 591 && mouseY > 370 && mouseX < 629 && mouseY < 438 && mouseDown
					&& currentScene.equals("Atrium")) {
				currentScene = "Foyer";
				playWav(doorSFX);
				TimerAsOfLastClick = metroGnome;
				playWav(footstepsSFX);
				tooltip = "What a nice Foyer.";
			}
			if (mouseDown && mouseX > 720 && mouseY > 228 && mouseX < 780 && mouseY < 254
					&& currentScene.equals("Main Hall")) {
				currentScene = "Foyer";
				playWav(doorSFX);
				playWav(footstepsSFX);
				tooltip = "It's the Foyer.";
			}
			if (mouseDown && currentScene.equals("Foyer") && (TimerAsOfLastClick + 1) < metroGnome) {
				if (mouseX > 563 && mouseY > 368 && mouseX < 626 && mouseY < 418) {
					currentScene = "Main Hall";
					playWav(doorSFX);
					playWav(footstepsSFX);
					tooltip = "I'm back in the hallway.";
				}

				if (mouseX > 361 && mouseY > 160 && mouseX < 412 && mouseY < 218) {
					currentScene = "Atrium";
					playWav(doorSFX);

					playWav(footstepsSFX);
					tooltip = "This is the the top of the stairs.";
				}
			}

			if (mouseDown && mouseX > 94 && mouseY > 367 && mouseX < 154 && mouseY < 406
					&& currentScene.equals("Atrium") && (TimerAsOfLastClick + 1) < metroGnome) {
				TimerAsOfLastClick = metroGnome;
				if (areTheLightsOnInThePaintingRoom) {
					currentScene = "Painting Room";
					playWav(doorSFX);
					playWav(footstepsSFX);
					tooltip = "It's an ugly room.";

				} else {
					TimerAsOfLastClick = metroGnome;
					currentScene = "A dark void";
					playWav(doorSFX);
					playWav(footstepsSFX);
					tooltip = "It's a dark room.";
				}

			}

			if (mouseDown && mouseX > 460 && mouseY > 270 && mouseX < 500 && mouseY < 310
					&& currentScene.equals("Courtyard") && (TimerAsOfLastClick + 1) < metroGnome && frontDoorOpen) {
				TimerAsOfLastClick = metroGnome;
				setRainVolume(0.5f);
				currentScene = "Main Hall";
				playWav(omenSFX);
				tooltip = "It's a hallway.";

			}

			// !!! NOTE:
			// mouse cursor position check is duplicated to leave ZERO logic code in the
			// draw method.

			if (mouseX > 620 && mouseY > 341 && mouseX < 670 && mouseY < 380 & mouseDown
					&& (TimerAsOfLastClick + 1) < metroGnome
					&& (currentScene.equals("A dark void") || currentScene.equals("Painting Room"))) {
				currentScene = "Atrium";
				TimerAsOfLastClick = metroGnome;
				playWav(doorSFX);
			}

			if (mouseDown && mouseX > 160 && mouseY > 230 && mouseX < 650 && mouseY < 340
					&& (currentScene.equals("A dark void") || currentScene.equals("Painting Room"))
					&& cursorType.equals("magnifying_glass")) {
				tooltip = "It's raining outside.";
			} else if (mouseDown && mouseX > 60 && mouseY > 320 && mouseX < 100 && mouseY < 380
					&& currentScene.equals("Painting Room") && cursorType.equals("magnifying_glass")) {
				tooltip = "It's a light switch.";
			} else if (mouseDown && mouseX > 670 && mouseY > 300 && mouseX < 700 && mouseY < 430
					&& (currentScene.equals("A dark void") || currentScene.equals("Painting Room"))
					&& cursorType.equals("magnifying_glass")) {
				tooltip = "It's the door to the hallway.";
			} else if (mouseDown && (currentScene.equals("A dark void")) && cursorType.equals("magnifying_glass")) {
				tooltip = "It's a room.";
			} else if (mouseDown && (currentScene.equals("Painting Room")) && cursorType.equals("magnifying_glass")) {
				tooltip = "It's an ugly room.";
			}

			if ((metroGnome % 28) == 5 && LatestRNG > 0.5 && thundered == false) {
				if (!inMenu) {
					playWav(thunderSFX);
				}
				TimerAsOfEffect = metroGnome + 3;
				thundered = true;
			}
			if ((metroGnome % 28) == 7 && LatestRNG > 0.5 && thundered == true) {

				thundered = false;
			}

			if (!MagnifyingGlassActive && mouseDown && TimerAsOfLastClick < metroGnome
					&& (mouseX > 475 && mouseY > 450 && mouseX < 630 && mouseY < 580)) {
				TimerAsOfLastClick = metroGnome;
				if (!currentKeyColour.equals("spoink")) {
					if (cursorType.equals("held_key")) {
						cursorType = "clicker";
					} else {
						cursorType = "held_key";
					}
				}
			}

			// the key in the globe

			if (currentScene.equals("Atrium") && mouseX > 500 && mouseY > 180 && mouseX < 530 && mouseY < 217
					&& mouseDown && MagnifyingGlassActive) {
				tooltip = "It's a purple key stabbed into a globe.";
			} else if (!pinCollected && currentKeyColour.equals("spoink") && currentScene.equals("Atrium")
					&& mouseX > 500 && mouseY > 180 && mouseX < 530 && mouseY < 217 && mouseDown
					&& !MagnifyingGlassActive) {
				pinCollected = true;
//				cursorType = "held_key";
				currentKeyColour = "pink";

				tooltip = "This was as easy as taking a key from a missing person.";
			}
			// the key in the coat 216, 250 to 236, 354
			if (!orangeKeyCollected && currentKeyColour.equals("spoink") && currentScene.equals("Main Hall")
					&& mouseX > 216 && mouseY > 250 && mouseX < 236 && mouseY < 354 && mouseDown
					&& !MagnifyingGlassActive) {
//				cursorType = "held_key";
				currentKeyColour = "orange";
				orangeKeyCollected = true;
				tooltip = "A key in the coat pocket!";
			}

		}
	}

	/**
	 * Author: R. A. Ward Does: DRAWS up the gameWindow. called repeatedly,
	 * obviously. Createdated: 16-Jun-2026
	 * 
	 * @throws Nothing!!!
	 */
	private void draw(Graphics2D g) {
		g.setFont(TimesNewHamster);
		g.setColor(Color.BLACK);

		g.fillRect(0, 0, WIDTH, HEIGHT);
		g.setColor(Color.RED);
		g.drawString("IF YOU'RE SEEING THIS,", 200, 150);
		g.drawString("AN ERROR HAS OCCURRED", 200, 200);
		g.setColor(Color.BLACK);
		switch (currentScene) {
		case "basement":
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, WIDTH, HEIGHT);
			g.setColor(Color.cyan);
			g.drawString("YOU COMPLETED THE GAME!!!!", 270, 370);
			break;
		case "Foyer":
			g.drawImage(room5, 0, 0, WIDTH, HEIGHT, null);
//			g.setColor(Color.WHITE);
//			g.fillRect(0, 0, WIDTH, HEIGHT);
//			g.setColor(Color.BLACK);
//			g.drawString("room under construction", 150, 200);
			if (mouseX > 563 && mouseY > 368 && mouseX < 626 && mouseY < 418) {
				g.drawImage(goto_1, 563, 368, 64, 64, null);
			} else {
				g.drawImage(goto_0, 563, 368, 64, 64, null);
			}
			if (mouseX > 361 && mouseY > 160 && mouseX < 412 && mouseY < 218) {
				g.drawImage(getRotatedImage(goto_1, 90), 361, 180, 64, -64, null);
			} else {
				g.drawImage(getRotatedImage(goto_0, 90), 361, 180, 64, -64, null);
			}

			if (!theBasementDoorIsOpenYouCanCompleteTheGame) {
				switch (numLocksRemaining) {
				case 3:
					g.drawImage(b_door_3, 0, 0, WIDTH, HEIGHT, null);
					break;
				case 2:
					g.drawImage(b_door_2, 0, 0, WIDTH, HEIGHT, null);
					break;
				case 1:
					g.drawImage(b_door_1, 0, 0, WIDTH, HEIGHT, null);
					break;
				case 0:
					g.drawImage(b_door_0, 0, 0, WIDTH, HEIGHT, null);
					break;

				default:
					break;
				}
			} else {
				g.drawImage(b_door_open, 0, 0, WIDTH, HEIGHT, null);
			}
			if (theBasementDoorIsOpenYouCanCompleteTheGame) {
				if (mouseX > 238 && mouseY > 379 && mouseX < 274 && mouseY < 423) {
					g.drawImage(getRotatedImage(goto_1, 90), 238, 379, 64, -64, null);
				} else {
					g.drawImage(getRotatedImage(goto_0, 90), 238, 379, 64, -64, null);
				}
			}
			break;
		case "blank":
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, WIDTH, HEIGHT);
			break;
		case "menu":
			g.drawImage(menu, 0, 0, WIDTH, HEIGHT, null);
			if (DrawThePreamble) {
				g.drawImage(credit, 0, 0, WIDTH, HEIGHT, null);
			}
			if (DrawTheSignature) {
				g.drawImage(signature, 0, 0, WIDTH, HEIGHT, null);
			}
			if (DrawTheButtons) {
				if (PlayButtonHover) {
					g.drawImage(playButton1, 0, -50, WIDTH, HEIGHT, null);
				} else {

					g.drawImage(playButton0, 0, -50, WIDTH, HEIGHT, null);
				}
				if (LoadButtonHover) {
					g.drawImage(loadButton1, 0, -50, WIDTH, HEIGHT, null);
				} else {

					g.drawImage(loadButton0, 0, -50, WIDTH, HEIGHT, null);
				}
			}

			g.setFont(TimesNewHamsterPro);
			g.setColor(Color.yellow);
			g.fillRect(10, 555, 540, 35);
			g.setColor(Color.blue);
			g.drawString("To load, press Load Save and use save slots 1, 2, or 3 on your keyboard.", 10, 566);
			g.drawString("To save, press S in-game and use save slots 1, 2, or 3 on your keyboard.", 10, 580);

			break;
		case "Painting Room":
			g.drawImage(room2, 0, 0, WIDTH, HEIGHT, null);
			if (!PaintingMoved) {
				g.drawImage(painting, 0, 0, WIDTH, HEIGHT, null);
			} else {
				g.drawImage(cutout, 0, 0, WIDTH, HEIGHT, null);
				g.drawImage(painting, -150, 0, WIDTH, HEIGHT, null);
				if (!greenKeyCollected) {
					g.drawImage(key_green, 447, 115, 64, 64, null);
				}
			}
			break;
		case "A dark void":
			g.drawImage(room1, 0, 0, WIDTH, HEIGHT, null);
			break;
		case "Atrium":
			g.drawImage(room3, 0, -75, WIDTH, HEIGHT, null);
			if (mouseX > 94 && mouseY > 367 && mouseX < 154 && mouseY < 406) {
				g.drawImage(goto_1, 157, 353, -64, 64, null);
			} else if (currentScene.equals("Atrium")) {
				g.drawImage(goto_0, 157, 353, -64, 64, null);
			}
			if (mouseX > 641 && mouseY > 380 && mouseX < 624 && mouseY < 428) {
				g.drawImage(getRotatedImage(goto_1, 90), 641, 380, -64, 64, null);
			} else {
				g.drawImage(getRotatedImage(goto_0, 90), 641, 380, -64, 64, null);
			}
			break;

		case "Main Hall":
			g.drawImage(room4, 0, 0, WIDTH, HEIGHT, null);
			if (mouseX > 720 && mouseY > 228 && mouseX < 780 && mouseY < 254) {
				g.drawImage(goto_1, 786, 200, -64, 64, null);
			} else {
				g.drawImage(goto_0, 786, 200, -64, 64, null);
			}
			break;
		case "Courtyard":
			g.drawImage(outside0, 0, 0, WIDTH, HEIGHT, null);
			if ((TimerAsOfEffect + 1) > metroGnome && currentScene.equals("Courtyard") && !frontDoorOpen) {
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
		if (!currentScene.equals("basement")) {
			if (doorBellIsPressed && currentScene.equals("Courtyard")) {
				g.drawImage(overlay_doorbell, 0, 0, WIDTH, HEIGHT, null);
			}

			g.setColor(Color.WHITE);
			if (debugMode) {
				g.setColor(Color.MAGENTA);
				g.drawString("DEBUGGING", 20, 30);
				g.drawString("MouseX " + Integer.toString(mouseX), 20, 60);
				g.drawString("MouseY " + Integer.toString(mouseY), 20, 90);
				g.drawString("Game timer: " + Integer.toString(metroGnome), 20, 120);

				g.drawString("Mouse is being clicked: " + Boolean.toString(mouseDown), 20, 150);
				g.drawString("Magnifying Glass Active: " + Boolean.toString(MagnifyingGlassActive), 20, 180);
				g.drawString("Hungy? " + Boolean.toString(foundSomething), 20, 210);
				g.drawString("Last Click: " + Integer.toString(TimerAsOfLastClick), 20, 240);
				g.drawString("Scene: " + currentScene, 20, 270);
				g.drawString(
						"theBasementDoorIsOpenYouCanCompleteTheGame: " + theBasementDoorIsOpenYouCanCompleteTheGame, 20,
						300);
			}
			g.setColor(Color.WHITE);
//        (Math.random() * 5) +
			if ((TimerAsOfEffect + 1) > metroGnome && currentScene.equals("A dark void")) {
				g.drawImage(room1_lightning, 0, 0, WIDTH, HEIGHT, null);
			} else if ((TimerAsOfEffect + 1) > metroGnome && currentScene.equals("Atrium")) {
				g.drawImage(room3_lightning, 0, -75, WIDTH, HEIGHT, null);
			}

			// go-to arrows
			if (mouseX > 617 && mouseY > 324 && mouseX < 670 && mouseY < 380
					&& (currentScene.equals("Painting Room") || currentScene.equals("A dark void"))) {
				g.drawImage(goto_1, 617, 324, 64, 64, null);
			} else if (currentScene.equals("Painting Room") || currentScene.equals("A dark void")) {
				g.drawImage(goto_0, 617, 324, 64, 64, null);
			}
			if (!inMenu && !currentScene.equals("blank")) {
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
				if (!pinCollected && currentScene.equals("Atrium")) {
					g.drawImage(pin, 0, -75, WIDTH, HEIGHT, null);
				}

			}
			if (!currentScene.equals("blank") && !inMenu
//				&& !debugMode
			) {
				g.setColor(Color.WHITE);
				g.drawString("Location:", 653, 523);
				g.setFont(TimesNewHamsterPro);
				g.drawString(currentScene, 653, 553);
				g.setFont(TimesNewHamster);
				if (!cursorType.equals("held_key")) {
					if (currentKeyColour.equals("pink")) {
						g.drawImage(key_pink, 470, 450, 160, 160, null);
					}
					if (currentKeyColour.equals("orange")) {
						g.drawImage(key_orange, 470, 450, 160, 160, null);
					}
					if (currentKeyColour.equals("brass")) {
						g.drawImage(key_cursor, 470, 450, 160, 160, null);
					}
					if (currentKeyColour.equals("green")) {
						g.drawImage(key_green, 470, 450, 160, 160, null);
					}
				}
//			if (!currentKeyColour.equals("spoink")) {
//				switch (currentKeyColour) {
//				case "green":
//					break;
//				case "pink":
//					g.drawImage(key_pink, 470, 450, 160, 160, null);
//					break;
//				case "orange":
//					g.drawImage(key_orange, 470, 450, 160, 160, null);
//					break;
//				default:
//					break;
//
//				}
//			}
			}
		}
	}

	/**
	 * Author: R. A. Ward Does: Just draws the cursor. more efficient to use another
	 * mmethord for this. Createdated: 16-Jun-2026
	 */
	private void drawCursor(Graphics2D g) {

		int hotspotX = 0;

		int hotspotY = 0;
		if (cursorType.equals("clicker")) {
			if (foundSomething) {
				g.drawImage(cursor1, mouseX - hotspotX, mouseY - hotspotY, 64, 64, null);
			} else if (mouseDown) {
				g.drawImage(cursor2, mouseX - hotspotX, mouseY - hotspotY, 64, 64, null);
			} else {
				g.drawImage(cursor0, mouseX - hotspotX, mouseY - hotspotY, 64, 64, null);
			}
		}

		else if (cursorType.equals("magnifying_glass")) {
//    		if (mouseDown) {
			g.drawImage(magnifying_cursor, mouseX - hotspotX, mouseY - hotspotY, 64, 64, null);
//    		} else {
//    			g.drawImage(cursor0, mouseX - hotspotX, mouseY - hotspotY, 40, 40, null);
//    		}
		}

		else if (cursorType.equals("held_key")) {
			g.drawImage(key_cursor, mouseX - hotspotX, mouseY - hotspotY, 64, 64, null);

		} else if (cursorType.equals("menu")) {
			g.drawImage(cursor3, mouseX - hotspotX, mouseY - hotspotY, 64, 64, null);
		}

	}

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
						System.err.println("oops 1");
						e1.printStackTrace();
					} catch (UnsupportedAudioFileException e1) {
						System.err.println("oops 2");
						e1.printStackTrace();
					}
				} catch (IOException e) {
					System.err.println("oops 3");

					e.printStackTrace();
				}
			}).start();
		});
	}
}
//190, 247
//309, 450