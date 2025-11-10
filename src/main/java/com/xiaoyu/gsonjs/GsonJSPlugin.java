package com.xiaoyu.gsonjs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;

public class GsonJSPlugin implements KubeJSPlugin {
    public static final EventGroup GROUP = EventGroup.of("GsonJS");

    @Override
    public void registerBindings(BindingRegistry bindings) {
        bindings.add("GsonJS", new GsonWrapper());
    }

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(GROUP);
    }
}
