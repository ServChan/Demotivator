package org.lts.demotivator.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import org.lts.demotivator.gui.DemotivatorConfigScreen;

public final class DemotivatorModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return DemotivatorConfigScreen::new;
    }
}
