package io.github.derkottersberg.seamlessdeconstructor.neoforge.gametest;

import com.seamlessdeconstructor.SeamlessDeconstructorMod;
import com.seamlessdeconstructor.block.entity.ReverseDeconstructorBlockEntity;
import com.seamlessdeconstructor.gametest.WorkbenchGameTestScenario;
import com.seamlessdeconstructor.registry.ModBlocks;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public final class SeamlessDeconstructorNeoForgeGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS =
            DeferredRegister.create(BuiltInRegistries.TEST_FUNCTION, SeamlessDeconstructorMod.MOD_ID);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PROCESSING =
            registerFunction("processes_crafting_table", WorkbenchGameTestScenario::processesCraftingTableIntoIngredients);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ENCHANTMENTS =
            registerFunction("enchantment_book_atomicity", WorkbenchGameTestScenario::transfersEnchantmentsAndConsumesBookAtomically);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DAMAGED_INPUT =
            registerFunction("damaged_input", WorkbenchGameTestScenario::damagedInputUsesDurabilityAdjustedSalvage);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MODIFIED_BOOK =
            registerFunction("modified_book_rejected", WorkbenchGameTestScenario::rejectsModifiedBooksAsEnchantmentCarriers);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SAVE_RELOAD =
            registerFunction("pending_save_reload", WorkbenchGameTestScenario::blockedOperationSurvivesSaveReloadAndCommitsWithoutOverflow);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> AUTOMATION =
            registerFunction("sided_automation", WorkbenchGameTestScenario::exposesStableSidedAutomationRules);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> AUTOMATION_CAPABILITY =
            registerFunction("automation_capability", SeamlessDeconstructorNeoForgeGameTests::exposesRegisteredTransferHandlers);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SCREEN_SHIFT_CLICK =
            registerFunction("screen_shift_click", WorkbenchGameTestScenario::shiftClickRoutesBooksInputsAndOutputs);

    private SeamlessDeconstructorNeoForgeGameTests() {
    }

    public static void register(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
        modEventBus.addListener(SeamlessDeconstructorNeoForgeGameTests::registerTests);
    }

    private static DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> registerFunction(
            String name,
            Consumer<GameTestHelper> function) {
        return TEST_FUNCTIONS.register(name, () -> function);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                SeamlessDeconstructorMod.id("default_environment"),
                new TestEnvironmentDefinition.AllOf());
        registerTest(event, environment, "processes_crafting_table", PROCESSING, 650);
        registerTest(event, environment, "enchantment_book_atomicity", ENCHANTMENTS, 650);
        registerTest(event, environment, "damaged_input", DAMAGED_INPUT, 650);
        registerTest(event, environment, "modified_book_rejected", MODIFIED_BOOK, 200);
        registerTest(event, environment, "pending_save_reload", SAVE_RELOAD, 700);
        registerTest(event, environment, "sided_automation", AUTOMATION, 100);
        registerTest(event, environment, "automation_capability", AUTOMATION_CAPABILITY, 100);
        registerTest(event, environment, "screen_shift_click", SCREEN_SHIFT_CLICK, 100);
    }

    private static void registerTest(
            RegisterGameTestsEvent event,
            Holder<TestEnvironmentDefinition<?>> environment,
            String name,
            DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> function,
            int maxTicks) {
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(
                environment,
                Identifier.withDefaultNamespace("empty"),
                maxTicks,
                0,
                true);
        event.registerTest(
                SeamlessDeconstructorMod.id(name),
                new FunctionGameTestInstance(function.getKey(), data));
    }

    private static void exposesRegisteredTransferHandlers(GameTestHelper helper) {
        BlockPos relativePos = new BlockPos(1, 1, 1);
        helper.setBlock(relativePos, ModBlocks.REVERSE_DECONSTRUCTOR.get().defaultBlockState());
        ReverseDeconstructorBlockEntity blockEntity =
                helper.getBlockEntity(relativePos, ReverseDeconstructorBlockEntity.class);
        BlockPos absolutePos = helper.absolutePos(relativePos);

        ResourceHandler<ItemResource> unsided = helper.getLevel().getCapability(
                Capabilities.Item.BLOCK,
                absolutePos,
                blockEntity.getBlockState(),
                blockEntity,
                null);
        ResourceHandler<ItemResource> top = helper.getLevel().getCapability(
                Capabilities.Item.BLOCK,
                absolutePos,
                blockEntity.getBlockState(),
                blockEntity,
                Direction.UP);
        ResourceHandler<ItemResource> bottom = helper.getLevel().getCapability(
                Capabilities.Item.BLOCK,
                absolutePos,
                blockEntity.getBlockState(),
                blockEntity,
                Direction.DOWN);

        helper.assertTrue(unsided != null, "NeoForge unsided item transfer handler was not exposed");
        helper.assertTrue(top != null, "NeoForge input-side item transfer handler was not exposed");
        helper.assertTrue(bottom != null, "NeoForge output-side item transfer handler was not exposed");

        ItemStack modifiedBook = new ItemStack(Items.BOOK);
        modifiedBook.set(DataComponents.CUSTOM_NAME, Component.literal("Modified"));
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertValueEqual(
                    top.insert(1, ItemResource.of(modifiedBook), 1, transaction),
                    0,
                    "NeoForge transfer handler accepted a modified book");
        }
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertValueEqual(
                    top.insert(0, ItemResource.of(Items.CRAFTING_TABLE), 1, transaction),
                    1,
                    "NeoForge transfer handler did not accept a non-book input");
            transaction.commit();
        }
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertValueEqual(
                    top.insert(1, ItemResource.of(Items.BOOK), 5, transaction),
                    1,
                    "NeoForge transfer handler did not enforce one-book capacity");
            transaction.commit();
        }
        helper.assertValueEqual(
                blockEntity.getItem(ReverseDeconstructorBlockEntity.BOOK_SLOT).getCount(),
                1,
                "NeoForge transfer handler inserted the wrong number of books");

        blockEntity.setItem(
                ReverseDeconstructorBlockEntity.OUTPUT_START,
                new ItemStack(Items.OAK_PLANKS, 2));
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertValueEqual(
                    bottom.insert(0, ItemResource.of(Items.STONE), 1, transaction),
                    0,
                    "NeoForge output face accepted item insertion");
            helper.assertValueEqual(
                    top.extract(0, ItemResource.of(Items.CRAFTING_TABLE), 1, transaction),
                    0,
                    "NeoForge input face allowed input extraction");
            helper.assertValueEqual(
                    unsided.extract(ReverseDeconstructorBlockEntity.INPUT_SLOT, ItemResource.of(Items.CRAFTING_TABLE), 1, transaction),
                    0,
                    "NeoForge unsided access allowed input extraction");
            helper.assertValueEqual(
                    unsided.extract(ReverseDeconstructorBlockEntity.BOOK_SLOT, ItemResource.of(Items.BOOK), 1, transaction),
                    0,
                    "NeoForge unsided access allowed book extraction");
            helper.assertValueEqual(
                    unsided.extract(ReverseDeconstructorBlockEntity.OUTPUT_START, ItemResource.of(Items.OAK_PLANKS), 1, transaction),
                    1,
                    "NeoForge unsided access could not extract an output");
        }
        try (Transaction transaction = Transaction.openRoot()) {
            helper.assertValueEqual(
                    bottom.extract(0, ItemResource.of(Items.OAK_PLANKS), 1, transaction),
                    1,
                    "NeoForge output face could not extract an output");
        }
        helper.succeed();
    }
}
