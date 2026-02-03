# Testiplaan projektile „Börsibaar”

**Meeskond:** 26  
**Koodnimi:** SHAME

---

## 1. Testimise eesmärgid

Testimise eesmärk on tagada, et rakendus töötaks õigesti ja oleks eesmärgipäraselt kasutatav.

Testimise käigus soovime:
- avastada võimalikud vead varajases arenguetapis  
- tagada kriitiliste funktsioonide (login, POS, inventory) ootuspärase töötamise  
- veenduda, et kasutajad ei saaks ligi kaitstud lehtedele  

---

## 2. Testi levelid

### Unit Testing
Testime väikeseid koodijuppe, näiteks üksikuid funktsioone või teenuse loogikat.

### Integration Testing
Testime front- ja backendi koostööd, näiteks kas frontend saab backend API-st õiged andmed ja kas andmebaasiga suhtlus toimub korrektselt.

### System Testing
Testime süsteemi tervikuna, imiteerides kasutaja tavapärast käitumist (sisselogimine, toodete haldus, müük).

---

## 3. Testi skoop

### Skoobis
- Sisselogimine ja autentimine  
- Kaitstud lehed (dashboard, POS, inventory)  
- POS funktsionaalsus (müügi tegemine)  
- Inventaari haldus ja muutmine  
- Error handling  

### Skoobist väljas
- Jõudlus 
- Turvatestimine  
- Koormustestimine  

---

## 4. Testimisviis

- Peamiselt käsitsi testimine  
- Arendajad testivad oma arendusi ise  
- Lihtsad testjuhtumid, mis põhinevad päris kasutaja tegevustel  
- Vead parandatakse võimalusel kohe, kui need avastatakse  

---

## 5. Testimiskeskkond

- Local arenduskeskkond ja virtuaalserver  
- Dockeril põhinev seadistus (sama mis arenduses)  
- Erinevad veebilehitsejad nii arvutil kui ka mobiilil  

---

## 6. Sisenemis- ja väljumiskriteeriumid

### Sisenemiskriteeriumid (testimisega alustamine)
- Funktsionaalsus on implementeeritud  
- Rakendus kompileerub ja käivitub  
- Puuduvad blokeerivad kompileerimisvead  

### Väljumiskriteeriumid (testimisega lõpetamine)
- Põhifunktsioonid töötavad ilma kokkujooksmiseta  
- Kriitilisi vigu ei ole alles  
- Meeskond on nõus, et funktsioon on valmis  

---

## 7. Rollid ja vastutused

- **Arendajad:** implementeerivad funktsioone ja testivad ise oma koodi  
- **Mentor:** annab tagasisidet ja juhiseid  
- **Juht:** puudub  

---

## 8. Riskid ja eeldused

### Riskid
- Erinevad oskustasemed võivad põhjustada ebaühtlast testimist  
- Piiratud aeg testimiseks  
- Vead võivad ilmneda hilises etapis  

### Eeldused
- Backend API-d on stabiilsed  
- Meeskonnaliikmed testivad vastutustundlikult  

---

## 9. Testitulemused

- Käesolev testplaani dokument  
- Nimekiri testitud funktsioonidest  
- GitHubis dokumenteeritud ja parandatud vead
