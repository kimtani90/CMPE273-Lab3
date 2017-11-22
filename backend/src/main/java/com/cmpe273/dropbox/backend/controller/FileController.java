package com.cmpe273.dropbox.backend.controller;

import com.cmpe273.dropbox.backend.entity.Userfiles;
import com.cmpe273.dropbox.backend.entity.Users;
import com.cmpe273.dropbox.backend.service.FileService;
import com.cmpe273.dropbox.backend.service.UserFilesService;
import com.google.gson.Gson;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Controller    // This means that this class is a Controller
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping(path="/files") // This means URL's start with /demo (after Application path)
public class FileController {
    @Autowired
    private FileService fileService;

    @Autowired
    private UserFilesService userFilesService;

    //Save the uploaded file to this folder
    private static String UPLOADED_FOLDER = System.getProperty("user.dir") + "/public/uploads/";


    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "application/json")
    public ResponseEntity<com.cmpe273.dropbox.backend.entity.Files> fileupload(@RequestParam("file") MultipartFile multipartFile,
                                                                               @RequestParam("fileparent") String fileparent, HttpSession session) throws JSONException {

        String email = (String) session.getAttribute("email");

        com.cmpe273.dropbox.backend.entity.Files newFile = new com.cmpe273.dropbox.backend.entity.Files();

        try {

            String filepath = UPLOADED_FOLDER + email.split("\\.")[0] + "/" + multipartFile.getOriginalFilename();

            byte[] bytes = multipartFile.getBytes();
            Path path = Paths.get(filepath);
            Files.write(path, bytes);


            newFile.setFilename(multipartFile.getOriginalFilename());
            newFile.setFileparent(fileparent);
            newFile.setIsfile("T");
            newFile.setOwner(email);
            newFile.setSharedcount(0);
            newFile.setStarred("F");
            newFile.setFilepath(filepath);

            fileService.uploadFile(newFile);

            Userfiles userfiles = new Userfiles();

            userfiles.setEmail(email);
            userfiles.setFilepath(filepath);

            userFilesService.addUserFile(userfiles);


        } catch (IOException e) {
            e.printStackTrace();
        }


        return new ResponseEntity<com.cmpe273.dropbox.backend.entity.Files>(newFile, HttpStatus.OK);
    }

    @GetMapping(path = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<com.cmpe273.dropbox.backend.entity.Files>> getUserDetails(HttpSession session) {

        String email = (String) session.getAttribute("email");
        List<Userfiles> userFilesList = userFilesService.getUserFilesByEmail(email);

        List<com.cmpe273.dropbox.backend.entity.Files> filesList = new ArrayList<>();
        for (Userfiles userfiles : userFilesList) {

            com.cmpe273.dropbox.backend.entity.Files file = fileService.getFileByFilepath(userfiles.getFilepath());
            filesList.add(file);
        }

        return new ResponseEntity(filesList, HttpStatus.OK);
    }

    @PostMapping(path = "/delete", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteFile(@RequestBody com.cmpe273.dropbox.backend.entity.Files file, HttpSession session) throws JSONException {
        System.out.println(file.getFilepath());

        String email = (String) session.getAttribute("email");

        String filepath = UPLOADED_FOLDER + file.getOwner().split("\\.")[0] + "/" + file.getFilename();

        Path path = Paths.get(filepath);


        if (file.getOwner().equals(email)) {

            try {
                Files.delete(path);

                userFilesService.deleteUserFilesByFilepath(file.getFilepath());
                fileService.deleteFile(file.getFilepath());

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {

            userFilesService.deleteUserFilesByEmailAndFilepath(file.getFilepath(), email);
            fileService.updateSharedCount(file.getFilepath(), file.getSharedcount()+1);

        }

        return new ResponseEntity(null, HttpStatus.OK);

    }

    @PostMapping(path = "/sharefile", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> shareFile(@RequestBody String data, HttpSession session) throws JSONException {

        JSONObject jObject = new JSONObject(data);
        Gson gson = new Gson();
        JSONObject filedata = (JSONObject)jObject.get("filedata");
        com.cmpe273.dropbox.backend.entity.Files file = gson.fromJson(filedata.toString(), com.cmpe273.dropbox.backend.entity.Files.class);
        String shareEmail = jObject.getString("shareEmail");

        String email = (String) session.getAttribute("email");


        Userfiles userfiles = new Userfiles();

        userfiles.setEmail(shareEmail);
        userfiles.setFilepath(file.getFilepath());

        userFilesService.addUserFile(userfiles);

        fileService.updateSharedCount(file.getFilepath(), file.getSharedcount()+1);

        return new ResponseEntity(null, HttpStatus.OK);

    }

    @PostMapping(path = "/makefolder", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> shareFile(@RequestBody String data, HttpSession session) throws JSONException {

    }