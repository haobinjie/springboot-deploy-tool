package com.deploytool.controller;

import com.deploytool.AppContext;
import com.deploytool.model.BuildResult;
import com.deploytool.model.enums.LogCategory;
import com.deploytool.task.BuildTask;
import com.deploytool.task.TaskRunner;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

/**
 * 项目打包区控制器：选择目录、编辑打包命令、启动/终止打包，记录产物 jar。
 */
public class BuildController {

    private final AppContext ctx;

    private final TextField dirField = new TextField();
    private final Button chooseBtn = new Button("选择文件夹");
    private final TextField commandField = new TextField();
    private final Button startBtn = new Button("开始打包");
    private final Button stopBtn = new Button("终止打包");
    private final Label resultLabel = new Label("待打包");

    private BuildTask runningTask;
    private Path lastJar;
    private Runnable onProductReady; // 打包产物就绪后的回调（供部署区刷新）

    public BuildController(AppContext ctx) {
        this.ctx = ctx;
        // 回填全局配置
        commandField.setText(ctx.configService().getGlobalConfig().getBuildCommand());
        String savedDir = ctx.configService().getGlobalConfig().getProjectDir();
        if (savedDir != null) dirField.setText(savedDir);

        buildLayout();
        wireEvents();
        stopBtn.setDisable(true);
    }

    private void buildLayout() {
        HBox row1 = new HBox(8, new Label("项目目录"), dirField, chooseBtn);
        HBox row2 = new HBox(8, new Label("打包命令"), commandField);
        HBox row3 = new HBox(8, startBtn, stopBtn, resultLabel);
        dirField.setMaxWidth(Double.MAX_VALUE);
        commandField.setMaxWidth(Double.MAX_VALUE);
        javafx.scene.layout.HBox.setHgrow(dirField, javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.HBox.setHgrow(commandField, javafx.scene.layout.Priority.ALWAYS);
        this.rootNode = new VBox(10, row1, row2, row3);
    }

    private Node rootNode;

    public Node getRoot() { return rootNode; }

    private void wireEvents() {
        chooseBtn.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("选择 SpringBoot 项目根目录");
            File dir = chooser.showDialog(getRoot().getScene() == null ? null : getRoot().getScene().getWindow());
            if (dir != null) {
                dirField.setText(dir.getAbsolutePath());
                persistGlobal();
            }
        });

        startBtn.setOnAction(e -> startBuild());
        stopBtn.setOnAction(e -> {
            if (runningTask != null) runningTask.cancel();
        });
    }

    private void startBuild() {
        Path projectDir = Path.of(dirField.getText());
        String command = commandField.getText();
        if (dirField.getText().isBlank()) {
            ctx.logService().warn(LogCategory.SYSTEM, "请先选择项目目录");
            return;
        }
        resultLabel.setText("打包中…");
        startBtn.setDisable(true);
        stopBtn.setDisable(false);

        runningTask = new BuildTask(ctx, projectDir, command);
        runningTask.setOnSucceeded(e -> {
            BuildResult r = runningTask.getValue();
            onBuildFinished(r);
        });
        runningTask.setOnFailed(e -> {
            resultLabel.setText("打包失败");
            resetButtons();
        });
        runningTask.setOnCancelled(e -> {
            resultLabel.setText("已终止");
            resetButtons();
        });
        TaskRunner.run(runningTask);
    }

    private void onBuildFinished(BuildResult r) {
        resetButtons();
        if (r != null && r.isSuccess()) {
            lastJar = r.getJarPath();
            resultLabel.setText(lastJar != null ? "成功：" + lastJar.getFileName() : "成功：未识别jar");
            persistGlobal();
        } else {
            lastJar = null;
            resultLabel.setText("打包失败");
        }
        if (onProductReady != null) onProductReady.run();
    }

    private void resetButtons() {
        startBtn.setDisable(false);
        stopBtn.setDisable(true);
        runningTask = null;
    }

    private void persistGlobal() {
        var g = ctx.configService().getGlobalConfig();
        g.setProjectDir(dirField.getText());
        g.setBuildCommand(commandField.getText());
        ctx.configService().saveGlobal();
    }

    public Optional<Path> currentJar() {
        return Optional.ofNullable(lastJar);
    }

    public void setOnProductReady(Runnable onProductReady) {
        this.onProductReady = onProductReady;
    }

    public String currentProjectDir() {
        return dirField.getText();
    }

    public String currentCommand() {
        return commandField.getText();
    }
}
