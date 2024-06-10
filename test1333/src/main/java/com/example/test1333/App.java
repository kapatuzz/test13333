package com.example.test1333;

import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class App {
    public static void main(String[] args) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        Scanner scanner = new Scanner(System.in);
        // Відображення початкового використання пам'яті
        System.out.println("Початкове використання пам'яті:");
        FeatureMatchingUtils.printMemoryUsage(runtime);
        // Завантаження зображень
        MBFImage query = ImageUtilities.readMBF(new File("fff\\01.png"));
        MBFImage target = ImageUtilities.readMBF(new File("fff\\014.png"));
        System.out.println("Використання пам'яті після завантаження зображень:");
        FeatureMatchingUtils.printMemoryUsage(runtime);
        // Запит користувача для вибору методу
        System.out.println("Виберіть метод для використання:");
        System.out.println("1: Оптимізований код Beta(Best)");
        System.out.println("2: Неоптимізований код Beta");  
        int вибір = scanner.nextInt();
        scanner.close();
        // Виконання вибраного методу
        switch (вибір) {
            case 1:
                FeatureMatchingUtils.optimizedCodeBeta(runtime, query, target);
                break;
            case 2:
            	FeatureMatchingUtils.notOptimizedCodeBeta(runtime, query, target);
                break;          
            default:
                System.out.println("Неправильний вибір");
                break;
        }
    }
}