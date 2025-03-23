import javax.swing.*;

public class App {
    public static void main(String[] args) throws Exception {
        int boardWidth = 500;
        int boardHeight = 800;

        JFrame frame = new JFrame("Sky Glide");

		frame.setSize(boardWidth, boardHeight);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        BirdGame birdGame = new BirdGame();
        frame.add(birdGame);
        frame.pack();
        birdGame.requestFocus();
        frame.setVisible(true);
    }
}
