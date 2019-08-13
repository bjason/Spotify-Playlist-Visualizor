package image;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import grabber.Grabber;
import util.ImageCollageCreator;
import util.PlaylistIDManager;

@SuppressWarnings("unused")
public class ChartCollage extends Collage {
    public static final int TITLE_HEIGHT = 180;
    public static final int FOOTER_HEIGHT = 60;

    public ChartCollage(int x, int y, ImageCollageCreator creator) {
        super(x, y, BufferedImage.TYPE_INT_RGB);
        COLLAGE_X = x;
        COLLAGE_Y = y;
        this.creator = creator;
    }

    Graphics g;

    public Collage drawImages(ArrayList<HashMap<String, String>> inputImages) throws IOException {
        g = getGraphics();

        int x = 0;
        int y = TITLE_HEIGHT;
        int i = 0;
        int imageWidth = 0;

        for (HashMap<String, String> image : inputImages) {
            String fileName = getFileName(image, "");
            Cover cover = doResize(new Cover(fileName));

            g.drawImage(cover.getImage(), x, y, null);
            x += cover.IMAGE_X;
            imageWidth = cover.IMAGE_X;

            // draw detail section bar
            g.drawImage(cover.createDetailSection(image), x, y, null);

            x = 0;
            y += cover.IMAGE_Y;
            setProgress((int) (50 + i * ((float) 50 / inputImages.size())));
        }

        drawTitle(imageWidth);
        drawFooter(imageWidth);

        return this;
    }

    private void drawTitle(int imageWidth) {
        String title = Grabber.playlistName;
        BufferedImage section = new BufferedImage(Cover.DETAIL_X + imageWidth, TITLE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D background = section.createGraphics();

        background.setColor(Color.WHITE);
        background.fillRect(0, 0, Cover.DETAIL_X + imageWidth, TITLE_HEIGHT);
        background.dispose();

        background = section.createGraphics();
        background.setColor(Color.BLACK);
        background.setFont(new Font(Cover.EN_FONT, Font.BOLD, 70));
        background.drawString(title, 10, 100);

        g.drawImage(section, 0, 0, null);
    }

    private void drawFooter(int imageWidth) {
        BufferedImage section = new BufferedImage(Cover.DETAIL_X + imageWidth, FOOTER_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D background = section.createGraphics();

        background.setColor(Color.white);
        background.fillRect(0, 1, Cover.DETAIL_X + imageWidth, FOOTER_HEIGHT);
        background.dispose();

        background = section.createGraphics();
        background.setColor(Color.BLACK);
        background.setFont(new Font(Cover.EN_FONT, Font.PLAIN, 20));
        String softwareInfo = "Generated by Playlist Visualizor.";
        background.drawString(softwareInfo, 10, 30);
        String urlInfo = "Listen to this playlist: " + PlaylistIDManager.currentURL;
        background.drawString(urlInfo, 10, 50);

        g.drawImage(section, 0, COLLAGE_Y - FOOTER_HEIGHT, null);
    }

    public Collage drawImages(String[] inputImages) throws IOException {
        Graphics g = getGraphics();

        int x = 0;
        int y = 0;
        int i = 0;
        for (String image : inputImages) {
            Cover cover = doResize(new Cover(image));

            g.drawImage(cover.getImage(), x, y, null);
            x += cover.IMAGE_X;

            // draw detail section bar
            g.drawImage(cover.createDetailSection(), x, y, null);

            x = 0;
            y += cover.IMAGE_Y;
            setProgress(50 + i * (50 / inputImages.length));
        }
        return this;
    }
}
