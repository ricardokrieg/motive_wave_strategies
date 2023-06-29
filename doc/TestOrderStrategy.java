package ricardo_franco;

import com.motivewave.platform.sdk.common.Defaults;
import com.motivewave.platform.sdk.common.Enums.OrderAction;
import com.motivewave.platform.sdk.common.Enums.TIF;
import com.motivewave.platform.sdk.common.desc.SettingsDescriptor;
import com.motivewave.platform.sdk.order_mgmt.OrderContext;
import com.motivewave.platform.sdk.study.Study;
import com.motivewave.platform.sdk.study.RuntimeDescriptor;
import com.motivewave.platform.sdk.study.StudyHeader;


@StudyHeader(
    namespace="com.ricardofranco",
    id="TEST_ORDER_STRATEGY",
    name="Test Order Strategy",
    label="Test Order Strategy",
    desc="Test Order Strategy",
    menu="Ricardo Franco",
    overlay = true,
    studyOverlay = true,
    signals = true,
    strategy = true,
    autoEntry = true,
    manualEntry = true,
    supportsUnrealizedPL = true,
    supportsRealizedPL = true,
    requiresVolume = true,
    multipleInstrument = true,
    supportsBarUpdates = true,
    requiresBarUpdates = true,
    supportsUseAccountPosition = true,
    requiresUseAccountPosition = true,
    requiresBidAskHistory = true,
    supportsLongShort = true,
    supportsEntryPrice = true,
    supportsPosition = true,
    supportsCurrentPL = true,
    supportsTotalPL = true,
    supportsRiskRatio = true,
    supportsStopPL = true,
    supportsTargetPL = true,
    showTradeOptions = true,
    supportsTradeLots = true,
    supportsPositionType = true,
    supportsEnterOnActivate = true,
    supportsCloseOnDeactivate = true,
    supportsTIF = true,
    supportsSessions = true)
public class TestOrderStrategy extends Study {

    @Override
    public void initialize(Defaults defaults) {
        SettingsDescriptor sd = new SettingsDescriptor();
        setSettingsDescriptor(sd);

        RuntimeDescriptor desc = new RuntimeDescriptor();
        setRuntimeDescriptor(desc);
    }

    @Override
    public void onActivate(OrderContext ctx) {
        int tradeLots = getSettings().getTradeLots();
        int qty = tradeLots * ctx.getInstrument().getDefaultQuantity();

        // ctx.buy(qty);
        ctx.createStopOrder(OrderAction.BUY, TIF.DAY, qty, 1.12000f);
        ctx.createStopOrder(OrderAction.BUY, TIF.DAY, qty, 1.12100f);
        ctx.createStopOrder(OrderAction.BUY, TIF.DAY, qty, 1.12200f);
        ctx.createStopOrder(OrderAction.BUY, TIF.DAY, qty, 1.12300f);
        ctx.createStopOrder(OrderAction.BUY, TIF.DAY, qty, 1.12400f);
        ctx.createStopOrder(OrderAction.BUY, TIF.DAY, qty, 1.12500f);
        ctx.createStopOrder(OrderAction.BUY, TIF.DAY, qty, 1.12600f);
        ctx.createStopOrder(OrderAction.BUY, TIF.DAY, qty, 1.12700f);
        ctx.createStopOrder(OrderAction.BUY, TIF.DAY, qty, 1.12800f);
        ctx.createStopOrder(OrderAction.BUY, TIF.DAY, qty, 1.12900f);
        ctx.createStopOrder(OrderAction.BUY, TIF.DAY, qty, 1.13000f);

        ctx.createStopOrder(OrderAction.SELL, TIF.DAY, qty, 1.12000f);
        ctx.createStopOrder(OrderAction.SELL, TIF.DAY, qty, 1.12100f);
        ctx.createStopOrder(OrderAction.SELL, TIF.DAY, qty, 1.12200f);
        ctx.createStopOrder(OrderAction.SELL, TIF.DAY, qty, 1.12300f);
        ctx.createStopOrder(OrderAction.SELL, TIF.DAY, qty, 1.12400f);
        ctx.createStopOrder(OrderAction.SELL, TIF.DAY, qty, 1.12500f);
        ctx.createStopOrder(OrderAction.SELL, TIF.DAY, qty, 1.12600f);
        ctx.createStopOrder(OrderAction.SELL, TIF.DAY, qty, 1.12700f);
        ctx.createStopOrder(OrderAction.SELL, TIF.DAY, qty, 1.12800f);
        ctx.createStopOrder(OrderAction.SELL, TIF.DAY, qty, 1.12900f);
        ctx.createStopOrder(OrderAction.SELL, TIF.DAY, qty, 1.13000f);
    }
}
