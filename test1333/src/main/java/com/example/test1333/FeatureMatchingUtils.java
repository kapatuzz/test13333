package com.example.test1333;

import org.openimaj.feature.local.list.LocalFeatureList;
import org.openimaj.feature.local.matcher.BasicMatcher;
import org.openimaj.feature.local.matcher.FastBasicKeypointMatcher;
import org.openimaj.feature.local.matcher.LocalFeatureMatcher;
import org.openimaj.feature.local.matcher.MatchingUtilities;
import org.openimaj.feature.local.matcher.consistent.ConsistentLocalFeatureMatcher2d;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.feature.local.engine.DoGSIFTEngine;
import org.openimaj.image.feature.local.keypoints.Keypoint;
import org.openimaj.math.geometry.transforms.estimation.RobustAffineTransformEstimator;
import org.openimaj.math.model.fit.RANSAC;
import org.openimaj.util.pair.Pair;
import org.openimaj.image.colour.RGBColour;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FeatureMatchingUtils {
	public static void optimizedCodeBeta(Runtime runtime, MBFImage query, MBFImage target) throws IOException {
	    int numCols = 1; // стовпці 
	    int overlap = target.getWidth(); 
	    // Обчислення numRows на основі висоти target та query
	    int numRows = (int)  query.getHeight() / target.getHeight();
	    System.out.println(numRows);
	    int partWidth = query.getWidth() / numCols;
	    int partHeight = (query.getHeight() + (numRows - 1) * overlap) / numRows;

	    // Розділити зображення query на частини
	    List<MBFImage> queryParts = new ArrayList<>();
	    for (int row = 0; row < numRows; row++) {
	        for (int col = 0; col < numCols; col++) {
	            int x = col * partWidth;
	            int y = row * (partHeight - overlap);
	            MBFImage queryPart = query.extractROI(x, y, partWidth, partHeight);
	            queryParts.add(queryPart);
	        }
	    }

	    DoGSIFTEngine engine = new DoGSIFTEngine();
	    List<Pair<Keypoint>> allMatches = new ArrayList<>();
	    int maxMatches = 0;
	    int bestPartIndex = 0;

	    //Показ використання пам'яті після поділу на частини
	    System.out.println("Використання пам'яті після поділу на частини:");
	    printMemoryUsage(runtime);

	    // Обробити кожну частину зображення query
	    for (int i = 0; i < queryParts.size(); i++) {
	        int row = i / numCols;
	        int col = i % numCols;
	        int xOffset = col * partWidth;
	        int yOffset = row * (partHeight - overlap);

	        LocalFeatureList<Keypoint> queryKeypoints = engine.findFeatures(queryParts.get(i).flatten());
	        LocalFeatureList<Keypoint> targetKeypoints = engine.findFeatures(target.flatten());

	        // Відображення використання пам'яті після вилучення ознак
	        System.out.println("Використання пам'яті після вилучення ознак:");
	        printMemoryUsage(runtime);

	        // Виправлення координат ключових точок зображення query
	        for (Keypoint kp : queryKeypoints) {
	            kp.x += xOffset;
	            kp.y += yOffset;
	        }

	        // Створення та налаштування matcher
	        RobustAffineTransformEstimator modelFitter = new RobustAffineTransformEstimator(60.0, 2000, new RANSAC.PercentageInliersStoppingCondition(0.6));
	        LocalFeatureMatcher<Keypoint> basicMatcher = new BasicMatcher<>(100);
	        LocalFeatureMatcher<Keypoint> matcher = new ConsistentLocalFeatureMatcher2d<>(basicMatcher, modelFitter);

	        matcher.setModelFeatures(queryKeypoints);
	        matcher.findMatches(targetKeypoints);

	        // Display memory usage after creating the matcher
	        System.out.println("Використання пам'яті після створення matcher:");
	        printMemoryUsage(runtime);

	        // Applying Lowe's ratio test
	        List<Pair<Keypoint>> matches = matcher.getMatches();
	        List<Pair<Keypoint>> goodMatches = new ArrayList<>();
	        for (Pair<Keypoint> match : matches) {
	            Keypoint kp1 = match.firstObject();
	            Keypoint kp2 = match.secondObject();
	            double distance1 = calculateDistance(kp1.ivec, kp2.ivec);

	            double minDistance = Double.MAX_VALUE;
	            double secondMinDistance = Double.MAX_VALUE;
	            for (Keypoint kp : targetKeypoints) {
	                double distance = calculateDistance(kp1.ivec, kp.ivec);
	                if (distance < minDistance) {
	                    secondMinDistance = minDistance;
	                    minDistance = distance;
	                } else if (distance < secondMinDistance) {
	                    secondMinDistance = distance;
	                }
	            }

	            if (distance1 < 0.75 * secondMinDistance) {
	                goodMatches.add(match);
	            }
	        }

	        if (goodMatches.size() > maxMatches) {
	            maxMatches = goodMatches.size();
	            bestPartIndex = i;
	        }

	        // Збіги з найкращої частини
	        if (i == bestPartIndex) {
	            allMatches = new ArrayList<>(goodMatches);
	        }

	        // Відображення використання пам'яті після обробки частин зображення
	        System.out.println("Використання пам'яті після обробки частин зображення:");
	        printMemoryUsage(runtime);

	        queryKeypoints.clear();
	        targetKeypoints.clear();
	        System.gc(); // Запит на збирання сміття, щоб звільнити пам’ять
	    }

	    // Відображення збігів
	    MBFImage combinedMatches = MatchingUtilities.drawMatches(query, target, allMatches, RGBColour.RED);
	    DisplayUtilities.display(combinedMatches);

	    // Відображення використання пам'яті після відображення результатів
	    System.out.println("Використання пам'яті після відображення результатів:");
	    printMemoryUsage(runtime);

	    // Збереження отриманого зображення за вказаним шляхом
	    ImageUtilities.write(combinedMatches, new File("fff\\result.png"));

	    query = null;
	    target = null;
	    System.gc(); // Запит на збирання сміття, щоб звільнити пам’ять
	}

    public static void notOptimizedCodeBeta(Runtime runtime, MBFImage query, MBFImage target) {
        DoGSIFTEngine engine = new DoGSIFTEngine();
        LocalFeatureList<Keypoint> queryKeypoints = engine.findFeatures(query.flatten());
        LocalFeatureList<Keypoint> targetKeypoints = engine.findFeatures(target.flatten());

        // Відображення використання пам'яті після вилучення ознак
        System.out.println("Використання пам'яті після вилучення ознак:");
        printMemoryUsage(runtime);

        // Етап базового зіставлення
        LocalFeatureMatcher<Keypoint> matcher = new BasicMatcher<Keypoint>(80);
        matcher.setModelFeatures(queryKeypoints);
        matcher.findMatches(targetKeypoints);

        // Відображення використання пам'яті після першого етапу зіставлення
        System.out.println("Використання пам'яті після першого етапу зіставлення:");
        printMemoryUsage(runtime);

        // Етап послідовного зіставлення з використанням робастного афінного перетворювача
        RobustAffineTransformEstimator modelFitter = new RobustAffineTransformEstimator(50.0, 1500, new RANSAC.PercentageInliersStoppingCondition(0.5));
        matcher = new ConsistentLocalFeatureMatcher2d<Keypoint>(new FastBasicKeypointMatcher<Keypoint>(8), modelFitter);
        matcher.setModelFeatures(queryKeypoints);
        matcher.findMatches(targetKeypoints);

        // Відображення використання пам'яті після другого етапу зіставлення
        System.out.println("Використання пам'яті після другого етапу зіставлення:");
        printMemoryUsage(runtime);

        // Малювання співпадінь на оригінальному зображенні
        MBFImage consistentMatches = MatchingUtilities.drawMatches(query, target, matcher.getMatches(), RGBColour.RED);
        DisplayUtilities.display(consistentMatches);
     
    }

    private static double calculateDistance(byte[] vec1, byte[] vec2) {
        double sum = 0.0;
        for (int i = 0; i < vec1.length; i++) {
            double diff = vec1[i] - vec2[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    public static void printMemoryUsage(Runtime runtime) {
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        System.out.println("Загальна пам'ять: " + totalMemory / 1048576 + " МБ");
        System.out.println("Вільна пам'ять: " + freeMemory / 1048576 + " МБ");
        System.out.println("Використана пам'ять: " + usedMemory / 1048576 + " МБ");
        System.out.println();
    }
}
