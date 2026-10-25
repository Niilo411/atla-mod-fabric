package io.github.niilo411.elementalpaths.platform.event;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public record RegisterCommandsEvent(CommandDispatcher<CommandSourceStack> getDispatcher,
                                    CommandBuildContext getBuildContext,
                                    Commands.CommandSelection getCommandSelection) {}
