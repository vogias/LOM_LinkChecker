LOM_LinkChecker
===============

The LOM_linkchecker checks the URL exists in the **_technical.location_** element of the [IEEE LOM metadata](http://ltsc.ieee.org/wg12/files/LOM_1484_12_1_v1_Final_Draft.pdf) and stores the results in a mySQL database. The program reads a folder contains LOM metadata in XML format and automatically moves those files which include invalid URLs to another folder (let's say broken folder). The file, in our case, is invalid if the _HTTP RESPONSE_ of the URL is not 200.
If the file contains an invalid IEEE LOM metadata (e.g., does not include _technical.location_ element) is also moved to the broken folder. We name the file as 'ill-formed'.
