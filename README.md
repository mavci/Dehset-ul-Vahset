Dehşet-ül Vahşet / Android RPG - Alpha version
============================================

<img src="http://i.imgur.com/QB2Uz.png" alt="1">

Dehşet-ül Vahşet nedir?
---------

Dehşet-ül Vahşet açık kaynak kodlu, **Java** server üzerinden **Android** client ile oynanan, **TCP** ve **UDP** protokollerini kullanan ve oyuncu verilerini **MySQL**'de saklayan, alpha versiyonunda çok oyunculu rol yapma (multiplayer rpg) oyunudur.

Oyunun şu anki halinde belirgin bir amaç yoktur, fakat NPC ve oyuncu ihtiyaçlarını karşılayan yöntemlerin temeli hazırlanmıştır. Tamamen eğlence amaçlı, öylesine geliştirilip birilerinin içerisinde geçen bir takım tekniklerden faydalanıp işine yarayacağı düşüncesiyle açık kaynak olarak paylaşılıp merak edenlere sunulmuştur. Boş vakitlerde bulmaca gibi hataları çözüp, daha da geliştirerek eğlenmek ve bu şekilde eğlenirken de Java ve bir takım alt alanlarda tecrübe edinmek amacıyla yapılmıştır.

Neler var?
----------

Oyunda bir çok türde canavarlar mevcut; zombiler, sizi donduran büyücüler, peşinizi bırakmayan ya da sizden kaçan fareler ve çok güçlü canavarlar. Canavarları öldürdükçe para ve exp. kazanılır, eğer bir canavar tarafından öldürülürseniz tüm paranız sıfırlanır. Fakat oyunda henüz para işlevsel değil.

Rehine kurtarma ya da ganimet toplama gibi seçenekler de var.

Bir rehineyi canavarlara yem olmadan doğduğunuz bölgeye getirirseniz size ödül verilir.

Oyuna ilk başladığınız bölge aynı zamanda korumalı bölgedir ve orada hiç bir canavar size saldıramaz ve eğer canınız azalmışsa orada bekledikçe dolmaya başlar.

Harita gif dosyaları ile piksel-piksel çizerek kendi dilediğiniz haritayı tasarlayabilirsiniz. Haritada herhangi karede hangi yer döşemesi olacağını ya da dilediğiniz yerde hangi NPC nin olacağını belirleyebiliyorsunuz.

Neler yapılacak?
----------------

Başta da dediğim gibi oyunun genel olarak bir amacı yok. Canavarlara saldırılıp rehineler kurtarılabiliyor, para ve exp. biriktirilebiliyor. Bunların birleşiminden güzel bir senaryo ile bir hedef belirlenecek. Aynı zamanda temelin rahat ve esnek olmasından dolayı bir çok yeni NPC, canavar ve farklı görevlerde(rehine kurtarma gibi) eklenebilir.

Oyuncunun henüz bir çantası yok ve parasıyla bir şeyler alamıyor. Parayla eşya, iksir, silah ve kalkan gibi şeyler alınacak.

Ve sonrasında yeni canavarlar, yeni haritalar, yeni karakterler ve yeni eşyalar olacak.

Nasıl kuruluyor?
----------------

Henüz tam anlamıyla bir oyun ve amaç yapısına sahip olmadığı için hazırda hep açık olan bir serverı yok. Oyunu denemeniz için öncelikle server kurmanız gerekiyor. Server için gerekli şey MySQL veritabanı. Veritabanı oluşturulduktan sonra **game.sql** içerisindeki sorgu ile üyeler ve üyelerin verileri tablosu eklenir. Sonrasında veritabanı host, kullanıcı adı, şifre bilgileri servera girilir (bkz:DVServer.java). Daha sonrasında Android cliente de serverin IP adresi ve portları belirtilir (bkz: main.java). Şimdilik hepsi bu.

Harita dosyasının formatı nasıl?
--------------------------------

Harita dosyasında her bir piksel 32*32 lik bir alanı ifade eder. Her pikseldeki üç renk değerinden(RGB) kırmızı değeri(R) yerin spritedaki sıralamasını ifade eder. Örneğin kırmızı = 5 olan bir piksel oyun sprite dosyasındaki 5x0 karedeki sprite ifade eder. Yeşil değeri(G) ise NPC tip idsini ifade eder. NPClerin idleri şimdilik şu şekilde:

1.  RAT
2.  FOLLOWER RAT
3.  SPEED RAT
4.  ZOMBIE
5.  ICE_MAGE
6.  BULL
7.  MONEY
8.  GUARD
9.  TREASURE
1. HOSTAGE
2. HOLE

Yani eğer bir pikselin yeşil değeri 4 ise, o konumda zombi çıkar. Mavi ve şeffaflık değeri için henüz bir şey kullanılmıyor. Daha fazle detay için varolan map dosyasını inceleyebilirsiniz.
