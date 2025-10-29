/*
 * Rubus is a protocol for video and audio streaming and
 * the client and server reference implementations.
 * Copyright (C) 2025 Yegore Vlussove
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package frontend.spring;

import frontend.configuration.Config;
import frontend.interactors.WatchHistory;
import frontend.gui.settings.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;

@Configuration
public class SettingsTabsConfiguration {

	@Bean
	@Scope("prototype")
	@Order(0)
	@DependsOn("lookAndFeel")
	GeneralTabPanel generalTabPanel(Config config) {
		return new GeneralTabPanel(config);
	}

	@Bean
	@Scope("prototype")
	@Order(1)
	@DependsOn("lookAndFeel")
	ConnectionTabPanel connectionTabPanel(Config config, WatchHistory watchHistory) {
		return new ConnectionTabPanel(config, watchHistory);
	}

	@Bean
	@Scope("prototype")
	@Order(2)
	@DependsOn("lookAndFeel")
	PlayerTabPanel playerTabPanel(Config config) {
		return new PlayerTabPanel(config);
	}

	@Bean
	@Scope("prototype")
	@Order(3)
	@DependsOn("lookAndFeel")
	LocalTabPanel localTabPanel(WatchHistory watchHistory) {
		return new LocalTabPanel(watchHistory);
	}

	@Bean
	@Scope("prototype")
	@DependsOn("lookAndFeel")
	SettingsTabs settingsTabs(TabPanel[] tabPanels) {
		return new SettingsTabs(tabPanels);
	}
}
