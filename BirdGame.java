import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;
import javax.sound.sampled.*;
import java.io.IOException;

public class BirdGame extends JPanel implements ActionListener, KeyListener {
    int boardWidth = 500;
    int boardHeight = 800;

    // images
    Image backgroundImg;
    Image topPipeImg;
    Image bottomPipeImg;
    Image[] birdFrames = new Image[4]; // Bird flying animation frames

    // bird
    int birdX = boardWidth / 8;
    int birdY = boardWidth / 2;
    int birdWidth = 65;
    int birdHeight = 70;

    class Bird {
        int x = birdX;
        int y = birdY;
        int width = birdWidth;
        int height = birdHeight;
        Image[] frames;
        int currentFrameIndex = 0;
        int frameCount = 0; // counter to control frame change speed

        Bird(Image[] frames) {
            this.frames = frames;
        }

        void draw(Graphics g) {
            g.drawImage(frames[currentFrameIndex], x, y, width, height, null);
        }

        void updateAnimation() {
            frameCount++;
            if (frameCount % 10 == 0) { // Change frame every 10 ticks, adjust for desired speed
                currentFrameIndex = (currentFrameIndex + 1) % frames.length;
            }
        }
    }

    // pipe
    int pipeX = boardWidth;
    int pipeY = 0;
    int pipeWidth = 64;  // scaled by 1/6
    int pipeHeight = 512;

    class Pipe {
        int x = pipeX;
        int y = pipeY;
        int width = pipeWidth;
        int height = pipeHeight;
        Image img;
        boolean passed = false;

        Pipe(Image img) {
            this.img = img;
        }
    }

    // game logic
    Bird bird;
    int velocityX = -4; // move pipes to the left speed (simulates bird moving right)
    int velocityY = 0; // move bird up/down speed
    int gravity = 1;

    ArrayList<Pipe> pipes;
    Random random = new Random();

    Timer gameLoop;
    Timer placePipeTimer;
    boolean gameOver = false;
    double score = 0;

    // Sound
    private Clip bgm;
    private Clip explosionSound;

    BirdGame() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setFocusable(true);
        addKeyListener(this);

        // load images
        backgroundImg = new ImageIcon(getClass().getResource("./bg.png")).getImage();
        topPipeImg = new ImageIcon(getClass().getResource("./toppipe.png")).getImage();
        bottomPipeImg = new ImageIcon(getClass().getResource("./bottompipe.png")).getImage();

        // Load bird animation frames
        loadBirdAnimation();

        // bird with animated frames
        bird = new Bird(birdFrames);
        pipes = new ArrayList<Pipe>();

        // place pipes timer
        placePipeTimer = new Timer(1500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                placePipes();
            }
        });
        placePipeTimer.start();

        // game timer
        gameLoop = new Timer(1000 / 60, this); // how long it takes to start timer, milliseconds gone between frames
        gameLoop.start();

        loadSounds();
        playBGM();
    }

    private void loadBirdAnimation() {
        for (int i = 0; i < birdFrames.length; i++) {
            birdFrames[i] = new ImageIcon(getClass().getResource("./bird" + (i + 1) + ".png")).getImage();
        }
    }

    private void loadSounds() {
        try {
            // Load BGM
            AudioInputStream bgmStream = AudioSystem.getAudioInputStream(
                    getClass().getResource("/bgm.wav"));
            bgm = AudioSystem.getClip();
            bgm.open(bgmStream);

            // Load explosion sound
            AudioInputStream explosionStream = AudioSystem.getAudioInputStream(
                    getClass().getResource("/collide.wav"));
            explosionSound = AudioSystem.getClip();
            explosionSound.open(explosionStream);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private void playBGM() {
        if (bgm != null) {
            bgm.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    private void stopBGM() {
        if (bgm != null && bgm.isRunning()) {
            bgm.stop();
        }
    }

    private void playExplosionSound() {
        if (explosionSound != null) {
            explosionSound.setFramePosition(0);
            explosionSound.start();
        }
    }

    void placePipes() {
        int randomPipeY = (int) (pipeY - pipeHeight / 4 - Math.random() * (pipeHeight / 2));
        int openingSpace = boardHeight / 4;

        Pipe topPipe = new Pipe(topPipeImg);
        topPipe.y = randomPipeY;
        pipes.add(topPipe);

        Pipe bottomPipe = new Pipe(bottomPipeImg);
        bottomPipe.y = topPipe.y + pipeHeight + openingSpace;
        pipes.add(bottomPipe);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        // background
        g.drawImage(backgroundImg, 0, 0, boardWidth, boardHeight, null);

        // bird
        bird.draw(g);

        // pipes
        for (int i = 0; i < pipes.size(); i++) {
            Pipe pipe = pipes.get(i);
            g.drawImage(pipe.img, pipe.x, pipe.y, pipe.width, pipe.height, null);
        }

        // score
        g.setColor(Color.white);
        g.setFont(new Font("Arial", Font.PLAIN, 32));
        if (gameOver) {
            g.drawString("Game Over: " + String.valueOf((int) score), 10, 35);
        } else {
            g.drawString(String.valueOf((int) score), 10, 35);
        }
    }

    public void move() {
        // bird
        velocityY += gravity;
        bird.y += velocityY;
        bird.y = Math.max(bird.y, 0); // apply gravity to current bird.y, limit the bird.y to top of the canvas

        // Update bird animation
        bird.updateAnimation();

        // pipes
        for (int i = 0; i < pipes.size(); i++) {
            Pipe pipe = pipes.get(i);
            pipe.x += velocityX;

            if (!pipe.passed && bird.x > pipe.x + pipe.width) {
                score += 0.5; // 0.5 because there are 2 pipes! so 0.5*2 = 1, 1 for each set of pipes
                pipe.passed = true;
            }

            if (collision(bird, pipe)) {
                gameOver = true;
            }
        }

        if (bird.y > boardHeight) {
            gameOver = true;
        }
    }

    boolean collision(Bird a, Pipe b) {
        Rectangle birdRect = new Rectangle(a.x, a.y, a.width, a.height);

        // Reduce pipe width for more accurate collision detection
        Rectangle pipeRect = new Rectangle(b.x + 10, b.y, b.width - 20, b.height);

        boolean collided = birdRect.intersects(pipeRect);

        if (collided) {
            playExplosionSound();
        }

        return collided;
    }


    @Override
    public void actionPerformed(ActionEvent e) { // called every x milliseconds by gameLoop timer
        move();
        repaint();
        if (gameOver) {
            placePipeTimer.stop();
            gameLoop.stop();
            stopBGM();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            velocityY = -9;

            if (gameOver) {
                // restart game by resetting conditions
                bird.y = birdY;
                velocityY = 0;
                pipes.clear();
                gameOver = false;
                score = 0;
                gameLoop.start();
                placePipeTimer.start();
                playBGM();
            }
        }
    }

    // not needed
    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}
}
