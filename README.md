# QRGenerator
QRGenerator for converting URLs to QR with the help of the AWS services

TODO:
- add the DynamoDB dependency and initialize the database


Database tables:
URL
-uid
-description
-vanilla_url
-result
-timestamp
-deleteflag

Log
-uid
-timestamp
-action

--################## Notes ################
Upon uploading a text file, parse it and create rows in the URL and Log tables

Upon entering the name of the URL, generate the QR and save the image as a blob file in the URL table

Visualize the QR, given the name of the desired url is entered

upload a .txt file on the S3 
=>
parse it in the application using lambda to seperate the URLs
=>
use the zebra libarary to generate the QRs
=>
show on the FE the selected QR
