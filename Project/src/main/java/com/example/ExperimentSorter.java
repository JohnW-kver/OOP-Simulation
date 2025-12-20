package com.example;

import java.util.ArrayList;
import java.util.List;

public class ExperimentSorter {
    public static final int SORT_BY_RANGE = 0;
    public static final int SORT_BY_SPEED = 1;
    public static final int SORT_BY_ANGLE = 2;
    public static final int SORT_BY_MASS = 3;

    public static List<ExperimentRecord> mergeSort(List<ExperimentRecord> experiments, int sortBy) {
        // Base case: list of 0 or 1 elements is already sorted
        if (experiments.size() <= 1) {
            return new ArrayList<ExperimentRecord>(experiments);
        }

        // Find the middle point
        int middle = experiments.size() / 2;

        // Split into two halves
        List<ExperimentRecord> leftHalf = new ArrayList<ExperimentRecord>();
        List<ExperimentRecord> rightHalf = new ArrayList<ExperimentRecord>();

        // Copy elements to left half (index 0 to middle-1)
        for (int i = 0; i < middle; i++) {
            leftHalf.add(experiments.get(i));
        }

        // Copy elements to right half (index middle to end)
        for (int i = middle; i < experiments.size(); i++) {
            rightHalf.add(experiments.get(i));
        }

        // Recursively sort both halves
        leftHalf = mergeSort(leftHalf, sortBy);
        rightHalf = mergeSort(rightHalf, sortBy);

        // Merge the sorted halves
        return merge(leftHalf, rightHalf, sortBy);
    }

    private static List<ExperimentRecord> merge(List<ExperimentRecord> left,
            List<ExperimentRecord> right, int sortBy) {
        List<ExperimentRecord> result = new ArrayList<ExperimentRecord>();

        int leftIndex = 0;
        int rightIndex = 0;

        // Compare elements from both lists and add smaller one to result
        while (leftIndex < left.size() && rightIndex < right.size()) {
            ExperimentRecord leftRecord = left.get(leftIndex);
            ExperimentRecord rightRecord = right.get(rightIndex);

            // Compare based on sort criteria
            double leftValue = getValue(leftRecord, sortBy);
            double rightValue = getValue(rightRecord, sortBy);

            if (leftValue <= rightValue) {
                result.add(leftRecord);
                leftIndex++;
            } else {
                result.add(rightRecord);
                rightIndex++;
            }
        }

        // Add remaining elements from left list
        while (leftIndex < left.size()) {
            result.add(left.get(leftIndex));
            leftIndex++;
        }

        // Add remaining elements from right list
        while (rightIndex < right.size()) {
            result.add(right.get(rightIndex));
            rightIndex++;
        }

        return result;
    }

    private static double getValue(ExperimentRecord record, int sortBy) {
        if (sortBy == SORT_BY_RANGE) {
            return record.getLandedRange();
        } else if (sortBy == SORT_BY_SPEED) {
            return record.getSpeed();
        } else if (sortBy == SORT_BY_ANGLE) {
            return record.getAngle();
        } else if (sortBy == SORT_BY_MASS) {
            return record.getMass();
        } else {
            return record.getLandedRange(); // Default to range
        }
    }

    public static int binarySearchByRange(List<ExperimentRecord> experiments,
            double targetRange, double tolerance) {
        int left = 0;
        int right = experiments.size() - 1;

        while (left <= right) {
            int middle = (left + right) / 2;
            double currentRange = experiments.get(middle).getLandedRange();

            // Check if current range is within tolerance
            double difference = Math.abs(currentRange - targetRange);
            if (difference <= tolerance) {
                return middle; // Found a match
            }

            // If target is smaller, search in left half
            if (targetRange < currentRange) {
                right = middle - 1;
            } else {
                // If target is larger, search in right half
                left = middle + 1;
            }
        }

        return -1; // Not found
    }

    public static int binarySearchClosest(List<ExperimentRecord> experiments, double targetRange) {
        if (experiments.isEmpty()) {
            return -1;
        }

        int left = 0;
        int right = experiments.size() - 1;

        // Handle edge cases
        if (targetRange <= experiments.get(left).getLandedRange()) {
            return left;
        }
        if (targetRange >= experiments.get(right).getLandedRange()) {
            return right;
        }

        // Binary search
        while (left <= right) {
            int middle = (left + right) / 2;
            double currentRange = experiments.get(middle).getLandedRange();

            if (currentRange == targetRange) {
                return middle;
            }

            if (targetRange < currentRange) {
                right = middle - 1;
            } else {
                left = middle + 1;
            }
        }

        // After loop, left is the insertion point
        // Compare neighbors to find closest
        if (left >= experiments.size()) {
            return experiments.size() - 1;
        }
        if (left == 0) {
            return 0;
        }

        double leftDiff = Math.abs(experiments.get(left - 1).getLandedRange() - targetRange);
        double rightDiff = Math.abs(experiments.get(left).getLandedRange() - targetRange);

        if (leftDiff <= rightDiff) {
            return left - 1;
        } else {
            return left;
        }
    }

    public static List<ExperimentRecord> findExperimentsAboveRange(
            List<ExperimentRecord> experiments, double minRange) {
        List<ExperimentRecord> result = new ArrayList<ExperimentRecord>();

        if (experiments.isEmpty()) {
            return result;
        }

        // Binary search to find first experiment >= minRange
        int left = 0;
        int right = experiments.size() - 1;
        int firstIndex = experiments.size(); // Default to end (no matches)

        while (left <= right) {
            int middle = (left + right) / 2;
            double currentRange = experiments.get(middle).getLandedRange();

            if (currentRange >= minRange) {
                firstIndex = middle;
                right = middle - 1; // Look for earlier matches
            } else {
                left = middle + 1;
            }
        }

        // Add all experiments from firstIndex to end
        for (int i = firstIndex; i < experiments.size(); i++) {
            result.add(experiments.get(i));
        }

        return result;
    }

}
