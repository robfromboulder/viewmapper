// © 2024-2025 Rob Dickinson (robfromboulder)

package com.github.robfromboulder.viewmapper;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * CLI wrapper for ViewMapper agent.
 */
@Command(
        name = "viewmapper",
        version = "ViewMapper 478",
        description = "Dependency mapping agent for Trino views",
        mixinStandardHelpOptions = true,
        subcommands = {RunCommand.class}
)
public class Main implements Runnable {

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }

    @Override
    public void run() {
        // show help when no subcommand is specified
        CommandLine.usage(this, System.out);
    }

}
