package net.haesleinhuepf.clijx.tilor.implementations;

import net.haesleinhuepf.clij.macro.CLIJMacroPlugin;
import net.haesleinhuepf.clij2.AbstractCLIJ2Plugin;
import org.scijava.plugin.Plugin;

@Plugin(type = CLIJMacroPlugin.class, name = "CLIJxt_trainWekaModel")
public class TrainWekaModel extends AbstractTileWiseProcessableCLIJ2Plugin {

    public TrainWekaModel() {
        master = new net.haesleinhuepf.clijx.weka.TrainWekaModel();
    }
}