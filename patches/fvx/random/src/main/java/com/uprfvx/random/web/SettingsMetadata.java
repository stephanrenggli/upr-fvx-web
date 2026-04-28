package com.uprfvx.random.web;

import com.uprfvx.random.Settings;
import com.uprfvx.random.gui.MiscTweakStrings;
import com.uprfvx.romio.MiscTweak;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SettingsMetadata {
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("com/uprfvx/random/gui/Bundle");
    private static final Map<String, Location> LOCATIONS = new LinkedHashMap<>();
    private static final Set<String> INTERNAL_SETTINGS = Set.of("currentMiscTweaks", "romName");

    static {
        section("General Options", "General Options",
                "limitPokemon", "banIrregularAltFormes", "banPrematureEvos", "randomizeIntroMon", "raceMode");
        section("Pokemon Traits", "Pokemon Base Statistics",
                "baseStatisticsMod", "baseStatsFollowEvolutions", "baseStatsFollowMegaEvolutions", "assignEvoStatsRandomly",
                "updateBaseStats", "updateBaseStatsToGeneration", "standardizeEXPCurves", "expCurveMod", "selectedEXPCurve");
        section("Pokemon Traits", "Pokemon Types",
                "speciesTypesMod", "typesFollowMegaEvolutions", "dualTypeOnly");
        section("Pokemon Traits", "Pokemon Abilities",
                "abilitiesMod", "allowWonderGuard", "abilitiesFollowEvolutions", "abilitiesFollowMegaEvolutions",
                "banTrappingAbilities", "banNegativeAbilities", "banBadAbilities", "weighDuplicateAbilitiesTogether",
                "ensureTwoAbilities");
        section("Pokemon Traits", "Pokemon Evolutions",
                "evolutionsMod", "evosSimilarStrength", "evosSameTyping", "evosMaxThreeStages", "evosForceChange",
                "changeImpossibleEvolutions", "estimateLevelForEvolutionImprovements", "makeEvolutionsEasier",
                "makeEvolutionsEasierLvl", "removeTimeBasedEvolutions", "evosAllowAltFormes", "evosForceGrowth",
                "evosNoConvergence");
        section("Pokemon Traits", "Pokemon Palettes",
                "pokemonPalettesMod", "pokemonPalettesFollowTypes", "pokemonPalettesFollowEvolutions",
                "pokemonPalettesShinyFromNormal");
        section("Starters, Statics & Trades", "Starter Pokemon",
                "startersMod", "startersTypeMod", "startersSingleType", "startersNoDualTypes", "startersNoLegendaries",
                "randomizeStartersHeldItems", "banBadRandomStarterHeldItems", "allowStarterAltFormes",
                "startersBSTMinimum", "startersBSTMaximum");
        section("Starters, Statics & Trades", "Static Pokemon",
                "staticPokemonMod", "limitMainGameLegendaries", "limit600", "allowStaticAltFormes", "swapStaticMegaEvos",
                "staticLevelModified", "staticLevelModifier", "correctStaticMusic");
        section("Starters, Statics & Trades", "Totem Pokemon",
                "totemPokemonMod", "randomizeTotemHeldItems", "totemLevelsModified", "totemLevelModifier",
                "allowTotemAltFormes");
        section("Starters, Statics & Trades", "Ally Pokemon", "allyPokemonMod");
        section("Starters, Statics & Trades", "Auras", "auraMod");
        section("Starters, Statics & Trades", "In-Game Trades",
                "inGameTradesMod", "randomizeInGameTradesNicknames", "randomizeInGameTradesOTs",
                "randomizeInGameTradesIVs", "randomizeInGameTradesItems");
        section("Moves & Movesets", "Move Data",
                "updateMoves", "updateMovesLegacy", "updateMovesToGeneration", "randomizeMovePowers",
                "randomizeMoveAccuracies", "randomizeMovePPs", "randomizeMoveTypes", "randomizeMoveNames",
                "randomizeMoveCategory");
        section("Moves & Movesets", "Pokemon Movesets",
                "movesetsMod", "startWithGuaranteedMoves", "guaranteedMoveCount", "reorderDamagingMoves",
                "movesetsForceGoodDamaging", "movesetsGoodDamagingPercent", "blockBrokenMovesetMoves",
                "evolutionMovesForAll");
        section("Foe Pokemon", "Trainer Pokemon",
                "trainersMod", "rivalCarriesStarterThroughout", "trainersUsePokemonOfSimilarStrength",
                "trainersAvoidDuplicates", "trainersMatchTypingDistribution", "trainersBlockLegendaries",
                "trainersUseLocalPokemon", "trainersBlockEarlyWonderGuard", "trainersEnforceDistribution",
                "trainersEnforceMainPlaythrough", "randomizeTrainerNames", "randomizeTrainerClassNames",
                "trainersEvolveTheirPokemon", "trainersEvolutionLevelModifier", "trainersLevelModified",
                "trainersLevelModifier", "eliteFourUniquePokemonNumber", "allowTrainerAlternateFormes",
                "swapTrainerMegaEvos", "additionalBossTrainerPokemon", "additionalImportantTrainerPokemon",
                "additionalRegularTrainerPokemon", "randomizeHeldItemsForBossTrainerPokemon",
                "randomizeHeldItemsForImportantTrainerPokemon", "randomizeHeldItemsForRegularTrainerPokemon",
                "consumableItemsOnlyForTrainers", "sensibleItemsOnlyForTrainers", "highestLevelGetsItemsForTrainers",
                "diverseTypesForBossTrainers", "diverseTypesForImportantTrainers", "diverseTypesForRegularTrainers",
                "shinyChance", "betterBossTrainerMovesets", "betterImportantTrainerMovesets",
                "betterRegularTrainerMovesets");
        section("Wild Pokemon", "Wild Pokemon",
                "randomizeWildPokemon", "wildPokemonZoneMod", "splitWildZoneByEncounterTypes",
                "similarStrengthEncounters", "catchEmAllEncounters", "useTimeBasedEncounters",
                "blockWildLegendaries", "useMinimumCatchRate", "minimumCatchRateLevel",
                "randomizeWildPokemonHeldItems", "banBadRandomWildPokemonHeldItems", "balanceShakingGrass",
                "wildLevelsModified", "wildLevelModifier", "allowWildAltFormes");
        section("Wild Pokemon", "Evolution Restrictions",
                "wildPokemonEvolutionMod", "keepWildEvolutionFamilies");
        section("Wild Pokemon", "Type Restrictions",
                "wildPokemonTypeMod", "keepWildTypeThemes");
        section("TM/HMs & Tutors", "TM Moves",
                "tmsMod", "tmLevelUpMoveSanity", "keepFieldMoveTMs", "tmsForceGoodDamaging",
                "tmsGoodDamagingPercent", "blockBrokenTMMoves", "tmsFollowEvolutions");
        section("TM/HMs & Tutors", "TM/HM Compatibility", "tmsHmsCompatibilityMod", "fullHMCompat");
        section("TM/HMs & Tutors", "Move Tutor Moves",
                "moveTutorMovesMod", "tutorLevelUpMoveSanity", "keepFieldMoveTutors", "tutorsForceGoodDamaging",
                "tutorsGoodDamagingPercent", "blockBrokenTutorMoves", "tutorFollowEvolutions");
        section("TM/HMs & Tutors", "Move Tutor Compatibility", "moveTutorsCompatibilityMod");
        section("Items", "Field Items", "fieldItemsMod", "banBadRandomFieldItems");
        section("Items", "Shop Items", "shopItemsMod", "banBadRandomShopItems", "banRegularShopItems",
                "banOPShopItems");
        section("Items", "Special Shop Items", "guaranteeEvolutionItems", "guaranteeXItems",
                "balanceShopPrices", "addCheapRareCandiesToShops");
        section("Items", "Pickup Items", "pickupItemsMod", "banBadRandomPickupItems");
        section("Misc. Tweaks", "Type Effectiveness", "typeEffectivenessMod", "inverseTypesRandomImmunities",
                "updateTypeEffectiveness");
    }

    private SettingsMetadata() {
    }

    public static String schemaJson() {
        Map<String, GuiMetadata> metadataByName = GuiMetadata.load();
        StringBuilder json = new StringBuilder();
        json.append("{\"settings\":[");
        boolean first = true;

        for (Method method : settingsMethods()) {
            if (!first) {
                json.append(",");
            }
            first = false;
            appendMethodSetting(json, method, metadataByName);
        }

        for (MiscTweak tweak : MiscTweak.allTweaks) {
            if (!first) {
                json.append(",");
            }
            first = false;
            appendMiscTweak(json, tweak);
        }

        json.append("]}");
        return json.toString();
    }

    private static List<Method> settingsMethods() {
        List<Method> methods = new ArrayList<>();
        for (Method method : Settings.class.getMethods()) {
            if (method.getName().startsWith("set") && method.getParameterCount() == 1
                    && Modifier.isPublic(method.getModifiers())) {
                Class<?> type = method.getParameterTypes()[0];
                if ((type == boolean.class || type == int.class || type == String.class || type.isEnum())
                        && !INTERNAL_SETTINGS.contains(propertyName(method.getName()))) {
                    methods.add(method);
                }
            }
        }
        methods.sort(Comparator.comparing(Method::getName));
        return methods;
    }

    private static void appendMethodSetting(StringBuilder json, Method method, Map<String, GuiMetadata> metadataByName) {
        Class<?> type = method.getParameterTypes()[0];
        String name = propertyName(method.getName());
        Location location = LOCATIONS.get(name);
        if (location == null) {
            throw new IllegalStateException("Missing settings metadata location for " + name);
        }
        GuiMetadata metadata = metadataByName.get(name);

        json.append("{\"name\":\"").append(name).append("\",");
        json.append("\"setter\":\"").append(method.getName()).append("\",");
        json.append("\"type\":\"").append(schemaType(type)).append("\",");
        json.append("\"group\":\"").append(escapeJson(location.group)).append("\",");
        json.append("\"section\":\"").append(escapeJson(location.section)).append("\"");
        if (metadata != null) {
            if (metadata.label != null && !metadata.label.isBlank()) {
                json.append(",\"label\":\"").append(escapeJson(metadata.label)).append("\"");
            }
            if (metadata.tooltip != null && !metadata.tooltip.isBlank()) {
                json.append(",\"tooltip\":\"").append(escapeJson(cleanHtml(metadata.tooltip))).append("\"");
            }
        }

        if (type.isEnum()) {
            json.append(",\"values\":[");
            Object[] values = type.getEnumConstants();
            for (int i = 0; i < values.length; i += 1) {
                if (i > 0) {
                    json.append(",");
                }
                json.append("\"").append(((Enum<?>) values[i]).name()).append("\"");
            }
            json.append("]");
        }
        json.append("}");
    }

    private static void appendMiscTweak(StringBuilder json, MiscTweak tweak) {
        json.append("{\"name\":\"").append(miscTweakName(tweak)).append("\",");
        json.append("\"setter\":\"setCurrentMiscTweaks\",");
        json.append("\"type\":\"boolean\",");
        json.append("\"virtual\":true,");
        json.append("\"group\":\"Misc. Tweaks\",");
        json.append("\"section\":\"Misc. Tweaks\",");
        json.append("\"label\":\"").append(escapeJson(MiscTweakStrings.getName(tweak, BUNDLE))).append("\",");
        json.append("\"tooltip\":\"").append(escapeJson(cleanHtml(MiscTweakStrings.getToolTipText(tweak, BUNDLE)))).append("\"}");
    }

    private static void section(String group, String section, String... names) {
        for (String name : names) {
            LOCATIONS.put(name, new Location(group, section));
        }
    }

    private static String propertyName(String setter) {
        String name = setter.substring(3);
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private static String schemaType(Class<?> type) {
        if (type == boolean.class) {
            return "boolean";
        }
        if (type == int.class) {
            return "integer";
        }
        if (type == String.class) {
            return "string";
        }
        if (type.isEnum()) {
            return "enum";
        }
        return "unknown";
    }

    private static String miscTweakName(MiscTweak tweak) {
        return "miscTweak_" + tweak.getID();
    }

    private static String cleanHtml(String value) {
        return value
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</li>", "\n")
                .replaceAll("(?i)<li>", "- ")
                .replaceAll("<[^>]+>", "")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n\\s+", "\n")
                .trim();
    }

    private static String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i += 1) {
            char current = value.charAt(i);
            switch (current) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    escaped.append(current);
                    break;
            }
        }
        return escaped.toString();
    }

    private static final class Location {
        final String group;
        final String section;

        Location(String group, String section) {
            this.group = group;
            this.section = section;
        }
    }

    private static final class GuiMetadata {
        private static final Pattern COMPONENT_PATTERN = Pattern.compile(
                "<component[^>]*binding=\"([^\"]+)\"[\\s\\S]*?</component>"
        );
        private static final Pattern SETTER_PATTERN = Pattern.compile(
                "settings\\.(set[A-Z][A-Za-z0-9_]*)\\(([^;]+?)\\);",
                Pattern.DOTALL
        );
        private static final Pattern BINDING_PATTERN = Pattern.compile("([A-Za-z][A-Za-z0-9_]*)(?:\\.isSelected\\(|\\.getValue\\(|\\.getSelectedIndex\\()");

        final String label;
        final String tooltip;

        GuiMetadata(String label, String tooltip) {
            this.label = label;
            this.tooltip = tooltip;
        }

        static Map<String, GuiMetadata> load() {
            try {
                String form = readResource("com/uprfvx/random/web/RandomizerGUI.form.txt");
                String source = readResource("com/uprfvx/random/web/RandomizerGUI.java.txt");
                return parseSetterMetadata(source, parseFormMetadata(form));
            } catch (Exception exception) {
                throw new IllegalStateException("Could not load FVX GUI metadata resources.", exception);
            }
        }

        private static String readResource(String path) throws Exception {
            try (var in = SettingsMetadata.class.getClassLoader().getResourceAsStream(path)) {
                if (in == null) {
                    throw new IllegalStateException("Missing metadata resource: " + path);
                }
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        private static Map<String, GuiMetadata> parseFormMetadata(String form) {
            Map<String, GuiMetadata> metadata = new HashMap<>();
            Matcher matcher = COMPONENT_PATTERN.matcher(form);
            while (matcher.find()) {
                String binding = matcher.group(1);
                String component = matcher.group(0);
                String label = bundleValue(component, "text");
                String tooltip = bundleValue(component, "toolTipText");
                if (label != null || tooltip != null) {
                    metadata.put(binding, new GuiMetadata(label, tooltip));
                }
            }
            return metadata;
        }

        private static String bundleValue(String component, String property) {
            Matcher matcher = Pattern.compile(property + " resource-bundle=\"[^\"]+\" key=\"([^\"]+)\"").matcher(component);
            if (matcher.find()) {
                return BUNDLE.getString(matcher.group(1));
            }
            return null;
        }

        private static Map<String, GuiMetadata> parseSetterMetadata(String source, Map<String, GuiMetadata> byBinding) {
            Map<String, GuiMetadata> metadata = new HashMap<>();
            Matcher setterMatcher = SETTER_PATTERN.matcher(source);
            while (setterMatcher.find()) {
                String property = propertyName(setterMatcher.group(1));
                Matcher bindingMatcher = BINDING_PATTERN.matcher(setterMatcher.group(2));

                while (bindingMatcher.find()) {
                    GuiMetadata guiMetadata = byBinding.get(bindingMatcher.group(1));
                    if (guiMetadata != null) {
                        metadata.put(property, guiMetadata);
                        break;
                    }
                }
            }
            return metadata;
        }
    }
}
