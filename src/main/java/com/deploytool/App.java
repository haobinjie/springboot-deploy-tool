package com.deploytool;

import com.deploytool.ui.MainView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * JavaFX 应用入口。装配 AppContext 与主视图，负责窗口生命周期。
 */
public class App extends Application {

    private MainView mainView;

    @Override
    public void start(Stage stage) {
        AppContext ctx = AppContext.createDefault();

        mainView = new MainView(ctx);
        StackPane root = new StackPane(mainView);
        Scene scene = new Scene(root, 900, 680);

        // 可选样式表
        var css = getClass().getResource("/css/app.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        stage.setTitle("SpringBoot Deploy Tool");
        stage.setScene(scene);
        stage.setMinWidth(760);
        stage.setMinHeight(560);
        stage.setOnCloseRequest(e -> mainView.dispose());
        stage.show();

        ctx.logService().info(com.deploytool.model.enums.LogCategory.SYSTEM,
                "欢迎使用 SpringBoot 一键部署工具");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
