package gregtech.api.items;

import gregtech.common.render.IColorMultiplier;
import gregtech.common.render.IIconRegister;
import gregtech.common.render.newitems.IItemIconProvider;
import ic2.api.item.IElectricItem;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import gregtech.api.GregTech_API;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.SubTag;
import gregtech.api.enums.TC_Aspects.TC_AspectStack;
import gregtech.api.interfaces.IFoodStat;
import gregtech.api.interfaces.IItemBehaviour;
import gregtech.api.interfaces.IItemContainer;
import gregtech.api.objects.ItemData;
import gregtech.api.util.GT_Config;
import gregtech.api.util.GT_LanguageManager;
import gregtech.api.util.GT_OreDictUnificator;
import gregtech.api.util.GT_Utility;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.awt.*;
import java.util.*;
import java.util.List;

import static gregtech.api.enums.GT_Values.*;

/**
 * @author Gregorius Techneticies
 *         <p/>
 *         One Item for everything!
 *         <p/>
 *         This brilliant Item Class is used for automatically generating all possible variations of Material Items, like Dusts, Ingots, Gems, Plates and similar.
 *         It saves me a ton of work, when adding Items, because I always have to make a new Item SubType for each OreDict Prefix, when adding a new Material.
 *         <p/>
 *         As you can see, up to 32766 Items can be generated using this Class. And the last 766 Items can be custom defined, just to save space and MetaData.
 *         <p/>
 *         These Items can also have special RightClick abilities, electric Charge or even be set to become a Food alike Item.
 */
public abstract class GT_MetaGenerated_Item extends GT_MetaBase_Item implements IElectricItem, IItemIconProvider, IIconRegister, IColorMultiplier {
    /**
     * All instances of this Item Class are listed here.
     * This gets used to register the Renderer to all Items of this Type, if useStandardMetaItemRenderer() returns true.
     * <p/>
     * You can also use the unlocalized Name gotten from getUnlocalizedName() as Key if you want to get a specific Item.
     */
    public static final HashMap<String, GT_MetaGenerated_Item> sInstances = new HashMap<String, GT_MetaGenerated_Item>();

	/* ---------- CONSTRUCTOR AND MEMBER VARIABLES ---------- */

    public final short mOffset, mItemAmount;
    public final BitSet mEnabledItems;
    public final BitSet mVisibleItems;

    @SideOnly(Side.CLIENT)
    public TextureAtlasSprite[][] mIconList;

    public final HashMap<Short, IFoodStat> mFoodStats = new HashMap<Short, IFoodStat>();
    public final HashMap<Short, Long[]> mElectricStats = new HashMap<Short, Long[]>();
    public final HashMap<Short, Long[]> mFluidContainerStats = new HashMap<Short, Long[]>();
    public final HashMap<Short, Short> mBurnValues = new HashMap<Short, Short>();

    /**
     * Creates the Item using these Parameters.
     *
     * @param aUnlocalized The Unlocalized Name of this Item.
     */
    public GT_MetaGenerated_Item(String aUnlocalized, short aOffset, short aItemAmount) {
        super(aUnlocalized);
        setCreativeTab(GregTech_API.TAB_GREGTECH_MATERIALS);
        setHasSubtypes(true);
        setMaxDamage(0);
        mEnabledItems = new BitSet(aItemAmount);
        mVisibleItems = new BitSet(aItemAmount);

        mOffset = (short) Math.min(32766, aOffset);
        mItemAmount = (short) Math.min(aItemAmount, 32766 - mOffset);

        sInstances.put(getUnlocalizedName(), this);
        invokeOnClient(() -> initClient());
    }



    @SideOnly(Side.CLIENT)
    public void initClient() {
        mIconList = new TextureAtlasSprite[mItemAmount][1];
    }

    /**
     * This adds a Custom Item to the ending Range.
     *
     * @param aID           The Id of the assigned Item [0 - mItemAmount] (The MetaData gets auto-shifted by +mOffset)
     * @param aEnglish      The Default Localized Name of the created Item
     * @param aToolTip      The Default ToolTip of the created Item, you can also insert null for having no ToolTip
     * @param aRandomData   The OreDict Names you want to give the Item. Also used for TC Aspects and some other things.
     * @return An ItemStack containing the newly created Item.
     */
    public final ItemStack addItem(int aID, String aEnglish, String aToolTip, Object... aRandomData) {
        if (aToolTip == null) aToolTip = "";
        if (aID >= 0 && aID < mItemAmount) {
            ItemStack rStack = new ItemStack(this, 1, mOffset + aID);
            mEnabledItems.set(aID);
            mVisibleItems.set(aID);
            GT_LanguageManager.addStringLocalization(getUnlocalizedName(rStack) + ".name", aEnglish);
            GT_LanguageManager.addStringLocalization(getUnlocalizedName(rStack) + ".tooltip", aToolTip);
            List<TC_AspectStack> tAspects = new ArrayList<TC_AspectStack>();
            // Important Stuff to do first
            for (Object tRandomData : aRandomData)
                if (tRandomData instanceof SubTag) {
                    if (tRandomData == SubTag.INVISIBLE) {
                        mVisibleItems.set(aID, false);
                        continue;
                    }
                    if (tRandomData == SubTag.NO_UNIFICATION) {
                        GT_OreDictUnificator.addToBlacklist(rStack);
                    }
                }
            // now check for the rest
            for (Object tRandomData : aRandomData)
                if (tRandomData != null) {
                    boolean tUseOreDict = true;
                    if (tRandomData instanceof IFoodStat) {
                        setFoodBehavior(mOffset + aID, (IFoodStat) tRandomData);
                        if (((IFoodStat) tRandomData).getFoodAction(this, rStack) == EnumAction.EAT) {
                            int tFoodValue = ((IFoodStat) tRandomData).getFoodLevel(this, rStack, null);
                            if (tFoodValue > 0)
                                RA.addCannerRecipe(rStack, ItemList.IC2_Food_Can_Empty.get(tFoodValue), ((IFoodStat) tRandomData).isRotten(this, rStack, null) ? ItemList.IC2_Food_Can_Spoiled.get(tFoodValue) : ItemList.IC2_Food_Can_Filled.get(tFoodValue), null, tFoodValue * 100, 1);
                        }
                        tUseOreDict = false;
                    }
                    if (tRandomData instanceof IItemBehaviour) {
                        addItemBehavior(mOffset + aID, (IItemBehaviour<GT_MetaBase_Item>) tRandomData);
                        tUseOreDict = false;
                    }
                    if (tRandomData instanceof IItemContainer) {
                        ((IItemContainer) tRandomData).set(rStack);
                        tUseOreDict = false;
                    }
                    if (tRandomData instanceof SubTag) {
                        continue;
                    }
                    if (tRandomData instanceof TC_AspectStack) {
                        ((TC_AspectStack) tRandomData).addToAspectList(tAspects);
                        continue;
                    }
                    if (tRandomData instanceof ItemData) {
                        if (GT_Utility.isStringValid(tRandomData))
                            GT_OreDictUnificator.registerOre(tRandomData, rStack);
                        else GT_OreDictUnificator.addItemData(rStack, (ItemData) tRandomData);
                        continue;
                    }
                    if (tUseOreDict) {
                        GT_OreDictUnificator.registerOre(tRandomData, rStack);
                    }
                }
            if (GregTech_API.sThaumcraftCompat != null)
                GregTech_API.sThaumcraftCompat.registerThaumcraftAspectsToItem(rStack, tAspects, false);
            return rStack;
        }
        return null;
    }

    /**
     * Sets a Food Behavior for the Item.
     *
     * @param aMetaValue    the Meta Value of the Item you want to set it to. [0 - 32765]
     * @param aFoodBehavior the Food Behavior you want to add.
     * @return the Item itself for convenience in constructing.
     */
    public final GT_MetaGenerated_Item setFoodBehavior(int aMetaValue, IFoodStat aFoodBehavior) {
        if (aMetaValue < 0 || aMetaValue >= mOffset + mEnabledItems.length()) return this;
        if (aFoodBehavior == null) mFoodStats.remove((short) aMetaValue);
        else mFoodStats.put((short) aMetaValue, aFoodBehavior);
        return this;
    }

    /**
     * Sets the Furnace Burn Value for the Item.
     *
     * @param aMetaValue the Meta Value of the Item you want to set it to. [0 - 32765]
     * @param aValue     200 = 1 Burn Process = 500 EU, max = 32767 (that is 81917.5 EU)
     * @return the Item itself for convenience in constructing.
     */
    public final GT_MetaGenerated_Item setBurnValue(int aMetaValue, int aValue) {
        if (aMetaValue < 0 || aMetaValue >= mOffset + mEnabledItems.length() || aValue < 0) return this;
        if (aValue == 0) mBurnValues.remove((short) aMetaValue);
        else mBurnValues.put((short) aMetaValue, aValue > Short.MAX_VALUE ? Short.MAX_VALUE : (short) aValue);
        return this;
    }

    /**
     * @param aMetaValue     the Meta Value of the Item you want to set it to. [0 - 32765]
     * @param aMaxCharge     Maximum Charge. (if this is == 0 it will remove the Electric Behavior)
     * @param aTransferLimit Transfer Limit.
     * @param aTier          The electric Tier.
     * @param aSpecialData   If this Item has a Fixed Charge, like a SingleUse Battery (if > 0).
     *                       Use -1 if you want to make this Battery chargeable (the use and canUse Functions will still discharge if you just use this)
     *                       Use -2 if you want to make this Battery dischargeable.
     *                       Use -3 if you want to make this Battery charge/discharge-able.
     * @return the Item itself for convenience in constructing.
     */
    public final GT_MetaGenerated_Item setElectricStats(int aMetaValue, long aMaxCharge, long aTransferLimit, long aTier, long aSpecialData, boolean aUseAnimations) {
        if (aMetaValue < 0 || aMetaValue >= mOffset + mEnabledItems.length()) return this;
        if (aMaxCharge == 0) mElectricStats.remove((short) aMetaValue);
        else {
            mElectricStats.put((short) aMetaValue, new Long[]{aMaxCharge, Math.max(0, aTransferLimit), Math.max(-1, aTier), aSpecialData});
            invokeOnClient(() -> setElectricStatsIcon(aMetaValue, aUseAnimations));
        }
        return this;
    }

    @SideOnly(Side.CLIENT)
    public void setElectricStatsIcon(int aMetaValue, boolean aUseAnimations) {
        if (aMetaValue >= mOffset && aUseAnimations)
            mIconList[aMetaValue - mOffset] = Arrays.copyOf(mIconList[aMetaValue - mOffset], Math.max(9, mIconList[aMetaValue - mOffset].length));
    }

    /**
     * @param aMetaValue     the Meta Value of the Item you want to set it to. [0 - 32765]
     *                       Use -1 if you want to make this Battery chargeable (the use and canUse Functions will still discharge if you just use this)
     *                       Use -2 if you want to make this Battery dischargeable.
     *                       Use -3 if you want to make this Battery charge/discharge-able.
     * @return the Item itself for convenience in constructing.
     */
    public final GT_MetaGenerated_Item setFluidContainerStats(int aMetaValue, long aCapacity, long aStacksize) {
        if (aMetaValue < 0 || aMetaValue >= mOffset + mEnabledItems.length()) return this;
        if (aCapacity < 0) mFluidContainerStats.remove((short) aMetaValue);
        else mFluidContainerStats.put((short) aMetaValue, new Long[]{aCapacity, Math.max(1, aStacksize)});
        return this;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemstack(ItemStack stack, int tintIndex) {
        return makeColor(getRGBa(stack, tintIndex));
    }

    private int makeColor(short[] rgba) {
        try {
            for(int i = 0; i < 4; i++)
                rgba[i] = (short) Math.max(0, rgba[i]);
            return new Color(rgba[0], rgba[1], rgba[2], rgba[3]).getRGB();
        } catch (IllegalArgumentException err) {
            return Color.WHITE.getRGB();
        }
    }


    /**
     * @return the Color Modulation the Material is going to be rendered with.
     */
    public short[] getRGBa(ItemStack aStack, int tint) {
        return Materials._NULL.getRGBA();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public TextureAtlasSprite getIcon(ItemStack stack, int pass) {
        double maxCharge = getMaxCharge(stack);
        if(maxCharge != 0) {
            double currentCharge = getCharge(stack);
            double chargePercentage = currentCharge / maxCharge;
            TextureAtlasSprite[] icons = mIconList[stack.getItemDamage() - mOffset];
            if(icons.length > 1) {
                int maxIcons = icons.length - 2;
                int iconIndex = (int) Math.ceil(maxIcons * chargePercentage);
                return icons[1 + iconIndex];
            }
        }
        return mIconList[stack.getItemDamage() - mOffset][0];
    }

    /* ---------- INTERNAL OVERRIDES ---------- */

    @Override
    public ActionResult<ItemStack> onItemRightClick(ItemStack aStack, World aWorld, EntityPlayer aPlayer, EnumHand hand) {
        IFoodStat tStat = mFoodStats.get((short) getDamage(aStack));
        if (tStat != null && aPlayer.canEat(tStat.alwaysEdible(this, aStack, aPlayer)))
            return ActionResult.newResult(EnumActionResult.PASS, aStack);
        return super.onItemRightClick(aStack, aWorld, aPlayer, hand);
    }

    @Override
    public int getMaxItemUseDuration(ItemStack aStack) {
        return mFoodStats.get((short) getDamage(aStack)) == null ? 0 : 32;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack aStack) {
        IFoodStat tStat = mFoodStats.get((short) getDamage(aStack));
        return tStat == null ? EnumAction.NONE : tStat.getFoodAction(this, aStack);
    }



    /*@Override
    public final ItemStack onEaten(ItemStack aStack, World aWorld, EntityPlayer aPlayer, EnumHand hand) {
        IFoodStat tStat = mFoodStats.get((short) getDamage(aStack));
        if (tStat != null) {
            aPlayer.getFoodStats().addStats(tStat.getFoodLevel(this, aStack, aPlayer), tStat.getSaturation(this, aStack, aPlayer));
            tStat.onEaten(this, aStack, aPlayer);
        }
        return aStack;
    }*/

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item var1, CreativeTabs aCreativeTab, List<ItemStack> aList) {
        for (int i = 0, j = mEnabledItems.length(); i < j; i++)
            if (mVisibleItems.get(i) || (D1 && mEnabledItems.get(i))) {
                Long[] tStats = mElectricStats.get((short) (mOffset + i));
                if (tStats != null && tStats[3] < 0) {
                    ItemStack tStack = new ItemStack(this, 1, mOffset + i);
                    setCharge(tStack, Math.abs(tStats[0]));
                    isItemStackUsable(tStack);
                    aList.add(tStack);
                }
                if (tStats == null || tStats[3] != -2) {
                    ItemStack tStack = new ItemStack(this, 1, mOffset + i);
                    isItemStackUsable(tStack);
                    aList.add(tStack);
                }
            }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public final void registerIcons(TextureMap aIconRegister) {
        System.out.println("Registering item icons");
        for (short i = 0, j = (short) mEnabledItems.length(); i < j; i++)
            if (mEnabledItems.get(i)) {
                for (byte k = 1; k < mIconList[i].length; k++) {
                    mIconList[i][k] = aIconRegister.registerSprite(new ResourceLocation(RES_PATH_ITEM + (GT_Config.troll ? "troll" : getUnlocalizedName() + "/" + i + "/" + k)));
                }
                mIconList[i][0] = aIconRegister.registerSprite(new ResourceLocation(RES_PATH_ITEM + (GT_Config.troll ? "troll" : getUnlocalizedName() + "/" + i)));
            }
    }

    @Override
    public final Long[] getElectricStats(ItemStack aStack) {
        return getElectricStats((short) aStack.getItemDamage());
    }

    public final Long[] getElectricStats(short aDamage) {
        return mElectricStats.get(aDamage);
    }

    @Override
    public final Long[] getFluidContainerStats(ItemStack aStack) {
        return mFluidContainerStats.get((short) aStack.getItemDamage());
    }

    @Override
    public int getItemEnchantability() {
        return 0;
    }

    @Override
    public boolean isBookEnchantable(ItemStack aStack, ItemStack aBook) {
        return false;
    }

    @Override
    public boolean getIsRepairable(ItemStack aStack, ItemStack aMaterial) {
        return false;
    }
}