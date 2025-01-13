package com.shanebeestudios.skbee.elements.recipe.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Section;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import com.shanebeestudios.skbee.SkBee;
import com.shanebeestudios.skbee.api.recipe.CookingRecipeType;
import com.shanebeestudios.skbee.api.recipe.RecipeUtil;
import com.shanebeestudios.skbee.api.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Event;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.SmokingRecipe;
import org.bukkit.inventory.recipe.CookingBookCategory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.entry.EntryValidator.EntryValidatorBuilder;
import org.skriptlang.skript.lang.entry.util.ExpressionEntryData;
import org.skriptlang.skript.log.runtime.SyntaxRuntimeErrorProducer;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Name("Recipe - Register Cooking Recipe")
@Description({"This section allows you to register any cooking recipe and define special properties.",
    "\n`id` = The ID for your recipe. This is used for recipe discovery and Minecraft's /recipe command.",
    "\n`result` = The resulting ItemStack of this recipe.",
    "\n`input` = The item the recipe requires as an input to output the result (Accepts an ItemStack or RecipeChoice) (Required).",
    "\n`cooktime` = How long the recipe will take to finish cooking before result is given (Optional).",
    "\n`experience` = The amount of experience gained when the recipe is finished cooking (Optional).",
    "Default cook times are, furnace = 10 seconds, smoking/blasting = 5 seconds and campfire = 30 seconds.",
    "\n`group` = You can define a group in which all recipes under this are sorted together in the recipe book (Optional).",
    "Examples of this in game are beds and wood types.",
    "\n`category` = Which category in the recipe book this recipe should appear within (Optional 1.19.4+).",
    "Valid category types are \"food\", \"blocks\", \"misc\", if no category is defined it defaults to \"misc\"."})
@Examples({"register new furnace recipe:",
    "\tid: \"sieve:gravel_to_sand\"",
    "\tresult: sand",
    "\tinput: gravel",
    "\tgroup: \"sieve\"",
    "\tcooktime: 1 minecraft day # 20 minutes",
    "\texperience: 6",
    "\tcategory: \"blocks\"",
    "",
    "register new campfire recipe:",
    "\tid: \"sieve:cobblestone_to_gravel\"",
    "\tresult: gravel",
    "\tinput: cobblestone",
    "\tgroup: \"sieve\"",
    "\tcategory: \"blocks\"",
    "",
    "register new smoking recipe:",
    "\tid: \"chef:beef_jerky\"",
    "\tresult: cooked mutton named \"&oBeef&r Jerky\"",
    "\tinput: rotten flesh",
    "\tcategory: \"food\"",
    "",
    "register a new blasting recipe:",
    "\tid: \"firery_sword\"",
    "\tresult: diamond sword of fire aspect named \"Flaming Sword\"",
    "\tinput: diamond sword"})
@Since("3.0.0")
public class SecRecipeCooking extends Section implements SyntaxRuntimeErrorProducer {

    private static final EntryValidatorBuilder ENTRY_VALIDATOR = EntryValidator.builder();
    private static final Map<String, CookingBookCategory> CATEGORY_MAP = new HashMap<>();
    private static final boolean DEBUG = SkBee.getPlugin().getPluginConfig().SETTINGS_DEBUG;

    static {
        Skript.registerSection(SecRecipeCooking.class, "register [a] [new] (furnace|1:smoking|2:blasting|3:campfire) recipe");
        ENTRY_VALIDATOR.addEntryData(new ExpressionEntryData<>("id", null, false, String.class));
        ENTRY_VALIDATOR.addEntryData(new ExpressionEntryData<>("result", null, false, ItemStack.class));
        ENTRY_VALIDATOR.addEntryData(new ExpressionEntryData<>("input", null, false, RecipeChoice.class));
        ENTRY_VALIDATOR.addEntryData(new ExpressionEntryData<>("group", null, true, String.class));
        ENTRY_VALIDATOR.addEntryData(new ExpressionEntryData<>("cooktime", null, true, Timespan.class));
        ENTRY_VALIDATOR.addEntryData(new ExpressionEntryData<>("experience", null, true, Number.class));
        if (RecipeUtil.HAS_CATEGORY) {
            ENTRY_VALIDATOR.addEntryData(new ExpressionEntryData<>("category", null, true, String.class));
            for (CookingBookCategory category : CookingBookCategory.values()) {
                CATEGORY_MAP.put(category.toString().toLowerCase(Locale.ROOT), category);
            }
        }
    }

    private Node node;
    private CookingRecipeType recipeType;
    private Expression<String> id;
    private Expression<ItemStack> result;
    private Expression<RecipeChoice> input;
    private Expression<String> category;
    private Expression<String> group;
    private Expression<Timespan> cookTime;
    private Expression<Number> experience;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult, SectionNode sectionNode, List<TriggerItem> triggerItems) {
        this.node = getParser().getNode();
        EntryContainer container = ENTRY_VALIDATOR.build().validate(sectionNode);
        if (container == null) return false;

        this.recipeType = CookingRecipeType.values()[parseResult.mark];
        this.id = (Expression<String>) container.getOptional("id", false);
        if (this.id == null) return false;
        this.result = (Expression<ItemStack>) container.getOptional("result", false);
        if (this.result == null) return false;
        this.input = (Expression<RecipeChoice>) container.getOptional("input", false);
        if (this.input == null) return false;
        this.category = (Expression<String>) container.getOptional("category", false);
        this.group = (Expression<String>) container.getOptional("group", false);
        this.cookTime = (Expression<Timespan>) container.getOptional("cooktime", false);
        this.experience = (Expression<Number>) container.getOptional("experience", false);
        return true;
    }

    @Override
    protected @Nullable TriggerItem walk(Event event) {
        execute(event);
        return super.walk(event, false);
    }

    private void execute(Event event) {
        String recipeId = this.id.getSingle(event);
        if (recipeId == null) {
            error("Missing id");
            return;
        }
        NamespacedKey namespacedKey = Util.getNamespacedKey(recipeId, false);
        ItemStack result = this.result.getSingle(event);
        // #getConvertedExpression() is used to prevent the famous 'UnparsedLiterals must be converted before use'
        RecipeChoice input = this.input.getSingle(event);

        int cookTime = this.recipeType.getCookTime();
        if (this.cookTime != null) {
            Timespan timespan = this.cookTime.getSingle(event);
            if (timespan != null) {
                cookTime = (int) timespan.getAs(Timespan.TimePeriod.TICK);
            } else {
                warning("Invalid cooktime, defaulting to recipe default: " + new Timespan(Timespan.TimePeriod.TICK, cookTime));
            }
        }
        float experience = 0;
        if (this.experience != null) {
            Number num = this.experience.getSingle(event);
            if (num != null) {
                experience = num.floatValue();
            } else {
                warning("Invalid experience, defaulting to 0");
            }
        }

        if (namespacedKey == null) {
            error("Invalid id: " + recipeId);
            return;
        } else if (result == null || !result.getType().isItem() || result.getType().isAir()) {
            error("Invalid result: " + result);
            return;
        } else if (input == null) {
            error("Invalid input: " + this.input.toString(event, true));
            return;
        }

        CookingRecipe<?> recipe = switch (this.recipeType) {
            case FURNACE -> new FurnaceRecipe(namespacedKey, result, input, experience, cookTime);
            case SMOKING -> new SmokingRecipe(namespacedKey, result, input, experience, cookTime);
            case BLASTING -> new BlastingRecipe(namespacedKey, result, input, experience, cookTime);
            case CAMPFIRE -> new CampfireRecipe(namespacedKey, result, input, experience, cookTime);
        };
        if (this.category != null) {
            String category = this.category.getSingle(event);
            if (category != null && CATEGORY_MAP.containsKey(category.toLowerCase(Locale.ROOT)))
                recipe.setCategory(CATEGORY_MAP.get(category.toLowerCase(Locale.ROOT)));
        }

        String recipeGroup = this.group != null ? this.group.getSingle(event) : null;
        if (recipeGroup != null && !recipeGroup.isBlank())
            recipe.setGroup(recipeGroup);
        Bukkit.removeRecipe(namespacedKey);
        Bukkit.addRecipe(recipe);
        if (DEBUG) RecipeUtil.logCookingRecipe(recipe);
    }

    @Override
    public @NotNull String toString(@Nullable Event e, boolean d) {
        return "register a new " + this.recipeType.toString().toLowerCase(Locale.ROOT) + " recipe";
    }

    @Override
    public Node getNode() {
        return this.node;
    }

}
