/*
 * This file is part of plugin-spi, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.launch.plugin;

import com.google.common.collect.ImmutableList;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import org.spongepowered.launch.util.ImmutableMapEntry;
import org.spongepowered.launch.util.MixinUtils;
import org.spongepowered.plugin.PluginFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

public final class PluginDiscovererService implements ITransformationService {

    private static final String NAME = "plugin_discoverer";
    private final PluginLoader pluginLoader;

    public PluginDiscovererService() {
        this.pluginLoader = new PluginLoader();
    }

    @Nonnull
    @Override
    public String name() {
        return PluginDiscovererService.NAME;
    }

    @Override
    public void initialize(final IEnvironment environment) {
        final Path gameDirectory = environment.getProperty(IEnvironment.Keys.GAMEDIR.get()).orElse(Paths.get("."));
        this.pluginLoader.initialize(gameDirectory);
    }

    @Override
    public void beginScanning(final IEnvironment environment) {
        //NOOP
    }

    @Override
    public List<Map.Entry<String, Path>> runScan(final IEnvironment environment) {
        this.pluginLoader.discoverResources();

        final List<Map.Entry<String, Path>> launchResources = new ArrayList<>();

        for (final Map.Entry<String, Collection<PluginFile>> resourcesEntry : this.pluginLoader.getResources().entrySet()) {
            final Collection<PluginFile> resources = resourcesEntry.getValue();
            launchResources.addAll(
                resources
                    .stream()
                    .filter(pluginFile -> pluginFile.getManifest().isPresent() && MixinUtils.getMixinConfigs(pluginFile.getManifest().get()).isPresent())
                    .map(pluginFile -> ImmutableMapEntry.of(pluginFile.getRootPath().getFileName().toString(), pluginFile.getRootPath()))
                    .collect(Collectors.toList())
            );
        }

        return launchResources;
    }

    @Override
    public void onLoad(final IEnvironment env, final Set<String> otherServices) {
        this.pluginLoader.discoverServices();
        this.pluginLoader.getServices().forEach((k, v) -> this.pluginLoader.getEnvironment().getLogger().info("Plugin language loader '{}' found.", k));
    }

    @Nonnull
    @Override
    public List<ITransformer> transformers() {
        return ImmutableList.of();
    }
}
