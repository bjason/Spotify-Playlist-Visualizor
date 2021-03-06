package util;

import javax.imageio.ImageIO;
import javax.swing.*;

import grabber.Grabber;
import grabber.SpotifyGrabber;
import image.Collage;
import image.Cover;
import image.ChartCollage;
import image.ZuneCollage;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
//TODO there are still some blank grid in the picture
//TODO add gradient effect

/// Picks x random images from directory y and stitches them into a single image, saved to file z until all images are used
public class ImageCollageCreator extends SwingWorker<Void, Void> {
	private final int COLLAGE_X = 1920; // TODO calculate these properly, based
										// on screen res
	private final int COLLAGE_Y = 1080;
	private int numTracks = 0;

	private final String ALLOWED_EXTENSION = ".jpg";
	public static final String sourceDir = System.getProperty("user.home") + File.separator + "Pictures"
			+ File.separator + "Spotify Playlist Visualizor" + File.separator + "Album art";
	static final String outputDir = System.getProperty("user.home") + File.separator + "Pictures" + File.separator
			+ "Spotify Playlist Visualizor" + File.separator + "Collages";

	protected String errorCode = null; // to avoid SwingWorkers missing
										// exception handling

	@Override
	protected Void doInBackground() throws Exception {
		try {
			createAndSaveImages();
		} catch (Exception e) {
			errorCode = "Could not generate album art collages. Error code: " + e.getMessage();
			e.printStackTrace();
		}
		return null;
	}

	// TODO change the imagesizecode to the actual size of the image
	private void createAndSaveImages() throws IOException {
		int imageSizeCode = Integer.parseInt(PropertiesManager.getProperty("imageSizeCode"));
		int sourceId = Integer.parseInt(PropertiesManager.getProperty("sourceId"));

		int size = 300;
		int baseSize = 0;

		Collage collage = null;
		String[] thisCollageImages = null;
		File outputFile = null;

		ArrayList<String> allImages = getImageFilenames(sourceDir);
		numTracks = Grabber.numOfTracks;

		switch (imageSizeCode) {
		case 0:
		case 1:
		case 2:
			size = SpotifyGrabber.SPOTIFY_IMAGE_SIZES[imageSizeCode];

			drawOrdinaryMode(size, collage, allImages);
			break;
		case 3:
			// Chart Mode
//			Collections.reverse(allImages);
			// make the playlist a countdown


//			thisCollageImages = getCollageImagesNames(allImages, numTracks);
			int collage_x = size + Cover.DETAIL_X;
			int collage_y = size * numTracks + ChartCollage.FOOTER_HEIGHT + ChartCollage.TITLE_HEIGHT;

			collage = new ChartCollage(collage_x, collage_y, this);
			collage.setCoverSize(size, size);

			outputFile = getOutputFilename(outputDir);
			drawAndSave(collage, Grabber.getAllTracksInfo(), outputFile);
			break;
		case 4:
			// Zune Mode
			baseSize = (size - 6 * ZuneCollage.INTERVAL) / 4;
			Collections.shuffle(allImages);

			thisCollageImages = getCollageImagesNames(allImages, numTracks);
			collage = new ZuneCollage(this).setBaseSize(baseSize, baseSize);

			outputFile = getOutputFilename(outputDir);
			drawAndSave(collage, thisCollageImages, outputFile);
			break;
		}
	}

	private void drawAndSave(Collage collage, String[] thisCollageImages, File outputFile) throws IOException {
		delPrevCollgs(outputFile);
		collage.drawImages(thisCollageImages).createAndSaveCollage(outputFile);
	}


	private void drawAndSave(Collage collage, ArrayList<HashMap<String, String>> allTracksInfo, File outputFile) throws IOException {
		ChartCollage cc = (ChartCollage)collage;
		cc.drawImages(allTracksInfo).createAndSaveCollage(outputFile);
	}

	private void drawOrdinaryMode(int size, Collage collage, ArrayList<String> allImages) throws IOException {
		drawOrdinaryMode(size, size, collage, allImages);
	}

	private void drawOrdinaryMode(int size_x, int size_y, Collage collage, ArrayList<String> allImages)
			throws IOException {
		String[] thisCollageImages;
		File outputFile = null;
		int count = 0;

		ArrayList<String> unusedImagesNames = new ArrayList<>(allImages);
		Collections.shuffle(allImages); // randomize order of images in
										// collages
		int imagesPerCollage = getImagesPerCollage(size_x, size_y);
		thisCollageImages = new String[imagesPerCollage];

		// roughly calculate progress for the loading bar
		int approxRequiredIterations = unusedImagesNames.size() / imagesPerCollage;

		while (unusedImagesNames.size() >= imagesPerCollage) {
			count++;
			// grab the next imagesPerCollage images from the shuffled
			// unused images

			for (int i = 0; i < imagesPerCollage; i++) {
				thisCollageImages[i] = unusedImagesNames.get(0);
				unusedImagesNames.remove(0);
				// as we use each image, remove it from the unused list
			}

			// generate a unique filename for each, just "collage x.jpg"
			outputFile = getOutputFilename(outputDir, count);

			collage = new Collage(this);
			System.out.println("size = " + size_x);
			collage.setCoverSize(size_x, size_y);
			drawAndSave(collage, thisCollageImages, outputFile);

			setProgress((int) (((double) count / approxRequiredIterations) * 100));
		}

		// if are some leftover images (ie unusedImages.size() %
		// imagesPerCollage != 0)
		// then use of the already used images to fill this collage
		if (unusedImagesNames.size() > 0) {
			collage = new Collage(this);
			collage.setCoverSize(size_x, size_y);

			count++;
			// start by using all remaining unused images
			ArrayList<String> lastCollageImages = new ArrayList<>(unusedImagesNames);
			int progToSet = 100 - getProgress();
			int progPerIterUnit = progToSet / (imagesPerCollage / lastCollageImages.size());
			int prog = 0;

			// then top up with any other images
			while (lastCollageImages.size() < imagesPerCollage) {
				int imagesToAdd = imagesPerCollage - lastCollageImages.size();
				if (imagesToAdd > allImages.size()) {
					imagesToAdd = allImages.size();
				}
				lastCollageImages.addAll(allImages.subList(0, imagesToAdd));
				Collections.shuffle(allImages);

				prog += progPerIterUnit;
				setProgress(prog);
			}

			outputFile = getOutputFilename(outputDir, count);
			thisCollageImages = lastCollageImages.toArray(new String[0]);
		}
		drawAndSave(collage, thisCollageImages, outputFile);
	}

	private String[] getCollageImagesNames(ArrayList<String> allImages, int numTracks) {
		String[] thisCollageImages = new String[numTracks];
		for (int count = 0; count < numTracks; count++) {
			thisCollageImages[count] = allImages.get(0);
			allImages.remove(0);

			setProgress(50 * (count / numTracks));
		}
		return thisCollageImages;
	}

	String cleanUpOutputFileName (String playlistName) {
		return playlistName.replaceAll("[\\\\/:*?\"<>|]", "_");
	}

	private File getOutputFilename(String outputDir) {
		String fileName = cleanUpOutputFileName(SpotifyGrabber.playlistName);
		return new File(outputDir + File.separator + "Albums in " + fileName + ".jpg");
	}

	private File getOutputFilename(String outputDir, int count) {
		return new File(outputDir + File.separator + "collage " + count + ".jpg");
	}

	private ArrayList<String> getImageFilenames(String inputDir) {
		File[] files = new File(inputDir).listFiles();

		if (files == null) {
			errorCode = "No directory built.";
			return null;
		} else {
			ArrayList<String> images = new ArrayList<>();
			for (File image : files) {
				if (image.isFile() && image.getAbsolutePath().endsWith(ALLOWED_EXTENSION)) {
					images.add(image.getName());
				}
			}
			return images;
			// only the image names
		}
	}

	private void delPrevCollgs(File file) {
		new File(outputDir).mkdirs(); // create the folders if they don't exist
		if (file.exists() && !file.isDirectory())
			file.delete();
	}

	public void publicSetProgress(int prog) {
		setProgress(prog);
	}

	private int getImagesPerCollage(int image_x, int image_y) {
		// calculate how many images can be fit in horizontally and vertically
		// round up so that there is no empty space at the edge of the wallpaper
		int x = (int) Math.ceil((double) COLLAGE_X / image_x);
		int y = (int) Math.ceil((double) COLLAGE_Y / image_y);
		return x * y;
	}
}