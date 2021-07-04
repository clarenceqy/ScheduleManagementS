package com.synir.controllers;

import com.synir.models.POJO.Schedule;
import com.synir.models.POJO.Timeblock;
import com.synir.models.POJO.UserSession;
import com.synir.models.Service.ScheduleInfoService;
import com.synir.utility.ApplicationContextUtils;
import com.synir.utility.FileUploadUtil;
import com.synir.utility.Helper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


@Component
public class AddScheduleVC {
    @Autowired
    ScheduleInfoService scheduleInfoService;
    @FXML
    private TextField titleField;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private AnchorPane companylistpane;
    @FXML
    private TableView<Timeblock> scheduletable;
    @FXML
    private ListView<String> companylistview;
    @FXML
    private TableColumn<Timeblock, String> header;
    @FXML
    private ChoiceBox levelcbox;
    @FXML
    private Button saveBtn;
    @FXML
    private Button deleteBtn;
    @FXML
    private Button screenshotBtn;
    @FXML
    private TableView<Timeblock> timeslotview;

    private ObservableList<Timeblock> timeblocklist;
    private ObservableList<Timeblock> timeslotlist;
    private ObservableList<String> companylist;
    private ObservableList<String> levellist;
    private Schedule schedule;
    private Helper helper;
    private UserSession session;

    public void initialize() {

        ApplicationContext applicationContext = ApplicationContextUtils.getApplicationContext();
        helper = (Helper) applicationContext.getBean("Helper");
        ArrayList<String> companyinput = helper.readCompany();
        companylist = FXCollections.observableArrayList();
        if (companyinput.size() != 0) {
            companylist.addAll(companyinput);
        }
        this.schedule = helper.readSchedule(helper.getScheduleUID());
        initializeSchecduleView();
        initializeCompanyView();
        draglocation();
        setScreenshot();
        titleField.setText(helper.getTitle());

        levellist = FXCollections.observableArrayList();
        session = (UserSession) applicationContext.getBean("UserSession");
        int curr_levl = session.getLevel();
        String[] levelname = {"权限5", "权限4", "权限3", "权限2", "权限1"};
        for (int i = 0; i < 5 - curr_levl; i++) {
            levellist.add(levelname[i]);
        }
        levellist.add("仅对上级可见");
        levelcbox.setItems(levellist);
        if (!session.getUsername().equals(helper.getCreator())) {
            saveBtn.setVisible(false);
            deleteBtn.setVisible(false);
        }
    }

    public void initializeSchecduleView() {
        scheduletable.getSelectionModel().setCellSelectionEnabled(true);

        header.setCellValueFactory(cellData -> cellData.getValue().timenameProperty());
        header.setSortable(false);
        for (int i = 1; i <= 31; i++) {
            TableColumn<Timeblock, String> temp = new TableColumn(Integer.toString(i));
            temp.setPrefWidth(69);
            temp.setSortable(false);
            int index = i - 1;
            temp.setCellValueFactory(cellData -> cellData.getValue().getLocationElement(index));
            temp.setCellFactory(col -> {
                TableCell<Timeblock, String> cell = new TableCell<Timeblock, String>() {
                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null) {
                            StringBuilder sb = new StringBuilder();
                            String[] arr = item.split("(?<=\\G.....)");
                            for (int i = 0; i < arr.length - 1; i++) {
                                sb.append(arr[i] + "\n");
                            }
                            if (arr.length <= 5){sb.append("\n");}
                            sb.append(arr[arr.length - 1]);
                            this.setText(sb.toString());
                        }
                    }
                };
                return cell;
            });
            scheduletable.getColumns().add(temp);
        }
        timeblocklist = FXCollections.observableArrayList();
        timeslotlist = FXCollections.observableArrayList();
        for (int i = 0; i < 12; i++) {
            timeblocklist.add(this.schedule.getSingleTimeBlock(i));
            timeslotlist.add(this.schedule.getSingleTimeBlock(i));
        }
        scheduletable.setItems(timeblocklist);
        scheduletable.setStyle("-fx-font-size :12");
        timeslotview.setItems(timeslotlist);
        timeslotview.setStyle("-fx-font-size :12");

    }

    public void initializeCompanyView() {

        companylistview.setItems(companylist);
        companylistpane.setMaxWidth(200.0);

    }

    public void draglocation() {

        companylistview.setOnDragDetected(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                int index = companylistview.getFocusModel().getFocusedIndex();
                String dragedcompany = companylist.get(index);

                Dragboard db = companylistview.startDragAndDrop(TransferMode.COPY);

                ClipboardContent content = new ClipboardContent();
                content.putString(dragedcompany);
                db.setContent(content);

                event.consume();
            }
        });

        scheduletable.setRowFactory(tv -> {
            TableRow<Timeblock> row = new TableRow<>();
            row.setOnDragOver(new EventHandler<DragEvent>() {
                @Override
                public void handle(DragEvent event) {
                    event.acceptTransferModes(TransferMode.COPY);
                    event.consume();
                }
            });

            row.setOnDragDropped(new EventHandler<DragEvent>() {
                @Override
                public void handle(DragEvent event) {
                    int rowidx = scheduletable.getFocusModel().getFocusedIndex();
                    int colidx = scheduletable.getFocusModel().getFocusedCell().getColumn();
                    if (colidx >= 0) {
                        String str = event.getDragboard().getString();
                        scheduletable.getItems().get(rowidx).setLocationElement(str, colidx );
                        scheduletable.refresh();
                    }
                    event.consume();
                }
            });
            row.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent event) {
                    int clickcount = event.getClickCount();
                    int rowidx = scheduletable.getFocusModel().getFocusedIndex();
                    int colidx = scheduletable.getFocusModel().getFocusedCell().getColumn();
                    if (clickcount == 2 && colidx >= 0) {
                        scheduletable.getItems().get(rowidx).setLocationElement("/", colidx);
                        scheduletable.refresh();
                    }
                    event.consume();
                }
            });
            return row;
        });
    }

    @FXML
    public void onAddCompanyBtnClicked() {
        Stage secondwindow = new Stage();
        BorderPane bd = new BorderPane();

        TextField textField = new TextField();
        Button confirmbutton = new Button();
        confirmbutton.setText("确认");
        textField.setEditable(true);
        textField.setText("请输入公司名");

        bd.setLeft(textField);
        bd.setRight(confirmbutton);

        Scene scene = new Scene(bd, 300, 30);
        secondwindow.setScene(scene);

        secondwindow.setTitle("添加公司");
        secondwindow.show();

        confirmbutton.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                String str = textField.getText();
                companylist.add(str);
                event.consume();
                secondwindow.close();
            }
        });
    }

    @FXML
    public void onDeleteCompanyBtnClicked() {
        int index = companylistview.getFocusModel().getFocusedIndex();
        companylist.remove(index);
        companylistview.refresh();
    }

    @FXML
    public void onSaveCompanyBtnClicked() {
        helper.writeCompany(this.companylist);
    }

    @FXML
    public void onSaveTableBtnClicked() throws Exception {
        String uuid = helper.getScheduleUID();
        helper.writeSchedule(this.timeblocklist, uuid);
        String l = levelcbox.getValue() == null ? "" : levelcbox.getValue().toString();
        helper.uploadSchedule(session.getUsername(), titleField.getText(), helper.computeLevel(session.getLevel(), l == null ? "" : l), uuid, helper.getId());
        FileUploadUtil ftp = new FileUploadUtil();
        File file = new File(helper.envpath + File.separator + helper.getScheduleUID() + ".dat");
        ftp.uploadFile(file, "/usr/local/testdat", helper.getScheduleUID() + ".dat");
        file.delete();
        //ftp.downloadSchedule("/usr/local/testdat","Company.dat");
    }

    public void setScreenshot() {
        screenshotBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {

                try {
                    SnapshotParameters param = new SnapshotParameters();
                    param.setDepthBuffer(true);
                    param.setFill(Color.CORNSILK);

                    WritableImage snapshot = scrollPane.snapshot(param, null);

                    BufferedImage tempImg = SwingFXUtils.fromFXImage(snapshot, null);

                    File outputfile = new File(helper.getEnvpath() + File.separator+ "First.png");
                    ImageIO.write(tempImg, "png", outputfile);


                } catch (IOException e) {
                    e.printStackTrace();
                }
                event.consume();
            }
        });

    }
}


