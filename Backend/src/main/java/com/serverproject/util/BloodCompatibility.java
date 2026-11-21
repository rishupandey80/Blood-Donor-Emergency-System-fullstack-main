package com.serverproject.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class BloodCompatibility {

    // key = patient (request) ka group, value = wo donor groups jo use blood de sakte hain
    private static final Map<String, Set<String>> DONORS_FOR;

    static {
        Map<String, Set<String>> m = new HashMap<>();
        m.put("O-", Set.of("O-"));
        m.put("O+", Set.of("O+", "O-"));
        m.put("A-", Set.of("A-", "O-"));
        m.put("A+", Set.of("A+", "A-", "O+", "O-"));
        m.put("B-", Set.of("B-", "O-"));
        m.put("B+", Set.of("B+", "B-", "O+", "O-"));
        m.put("AB-", Set.of("AB-", "A-", "B-", "O-"));
        m.put("AB+", Set.of("AB+", "AB-", "A+", "A-", "B+", "B-", "O+", "O-"));
        DONORS_FOR = Collections.unmodifiableMap(m);
    }

    public static String normalize(String group) {
        return group == null ? null : group.trim().toUpperCase();
    }

    public static boolean canDonate(String donorGroup, String requestGroup) {
        String donor = normalize(donorGroup);
        String request = normalize(requestGroup);
        if (donor == null || request == null) {
            return false;
        }
        Set<String> allowed = DONORS_FOR.get(request);
        return allowed != null && allowed.contains(donor);
    }

    public static Set<String> compatibleDonorGroups(String requestGroup) {
        Set<String> groups = DONORS_FOR.get(normalize(requestGroup));
        return groups == null ? Set.of() : new TreeSet<>(groups);
    }
}