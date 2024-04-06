package com.ocado.basket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BasketSplitter {
    private final Map<String, List<String>> deliveryForProducts;
    private final Set<String> deliveryMethods;

    public BasketSplitter(String absolutePathToConfigFile) throws ConfigurationException {
        //since configuration rarely changes, we read it once in the constructor rather than for each split invocation
        deliveryForProducts = ConfigurationReader.getDeliveryForProducts(absolutePathToConfigFile);
        deliveryMethods = deliveryForProducts.values().stream().flatMap(List::stream).collect(Collectors.toSet());
    }

    public Map<String, List<String>> split(List<String> items) {
        BasketSolution bestSolution = getBestSolutionForDeliveries(deliveryMethods, items);
        return bestSolution.getDeliveries();
    }

    private BasketSolution getBestSolutionForDeliveries(Set<String> deliveries, List<String> products) {
        if (deliveries.size() == 1) {
            return getSolutionForSingleDelivery(deliveries.iterator().next(), products);
        } else {
            return findBestSolutionForMultipleDeliveries(deliveries, products);
        }
    }

    private BasketSolution findBestSolutionForMultipleDeliveries(Set<String> deliveries, List<String> products) {
        BasketSolution bestScore = null;
        for (var deliveryMethod : deliveries) {
            var remainingDeliveries = deliveries.stream().filter(d -> !d.equals(deliveryMethod)).collect(Collectors.toSet());
            var productsBelongingToDelivery = partitionProductsForGivenDelivery(deliveryMethod, products);

            BasketSolution bestSolutionForRemainingDeliveries = getBestSolutionForDeliveries(remainingDeliveries, productsBelongingToDelivery.get(false));
            BasketSolution combinedSolution = BasketSolution.getSolutionWithAdditionalGroup(deliveryMethod, productsBelongingToDelivery.get(true), bestSolutionForRemainingDeliveries);
            if (bestScore == null || combinedSolution.isBetterThan(bestScore)) {
                bestScore = combinedSolution;
            }
        }
        return bestScore;
    }

    private BasketSolution getSolutionForSingleDelivery(String deliveryMethod, List<String> products) {
        Map<String, List<String>> map = new HashMap<>();
        if (!products.isEmpty()) {
            map.put(deliveryMethod, products);
        }
        return new BasketSolution(map, products.size());
    }

    private Map<Boolean, List<String>> partitionProductsForGivenDelivery(String deliveryMethod, List<String> products) {
        return products
                .stream()
                .collect(Collectors.partitioningBy(prod -> deliveryForProducts.get(prod).contains(deliveryMethod)));
    }
}