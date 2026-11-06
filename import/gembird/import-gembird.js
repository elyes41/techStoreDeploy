// =====================================================================
//  Import du catalogue GEMBIRD (SBC, 28/10/2026) dans la base TechStore
//  -> collections « categories » et « products » (mêmes champs que Category.java / Product.java)
//
//  Ré-exécutable sans risque : catégories repérées par slug, produits par référence (sku).
//  Un 2e passage met à jour prix, stock, nom, description, photo, catégorie
//  sans toucher aux ventes (soldCount), aux avis (rating), au slug, ni à active/featured.
//
//  Paramètres (variables d'environnement, facultatives) :
//    TVA=0.19        TVA ajoutée au prix HT du catalogue (le site affiche des prix TTC)
//    MARGE=0         marge appliquée sur le prix HT avant TVA (0.25 = +25 %)
//    DRY_RUN=1       affiche ce qui serait importé sans rien écrire
// =====================================================================

const TVA = Number(process.env.TVA ?? "0.19");
const MARGE = Number(process.env.MARGE ?? "0");
const DRY_RUN = process.env.DRY_RUN === "1";
if (!(TVA >= 0 && TVA < 1) || !(MARGE >= 0 && MARGE < 10)) throw new Error("TVA ou MARGE invalide");

const DATA = {
 "categories": [
  {
   "name": "Adaptateurs & convertisseurs",
   "slug": "adaptateurs-convertisseurs"
  },
  {
   "name": "Câbles vidéo",
   "slug": "cables-video"
  },
  {
   "name": "Hubs USB, cartes & acquisition",
   "slug": "hubs-usb-cartes-acquisition"
  },
  {
   "name": "Supports TV, meubles & projecteurs",
   "slug": "supports-tv-meubles-projecteurs"
  },
  {
   "name": "Bras & supports d'écran, PC portable",
   "slug": "bras-supports-d-ecran-pc-portable"
  },
  {
   "name": "Souris & microphones",
   "slug": "souris-microphones"
  },
  {
   "name": "Entretien",
   "slug": "entretien"
  }
 ],
 "products": [
  {
   "sku": "AB-HDMI-VGA-001",
   "name": "Adaptateur port HDMI vers VGA",
   "slug": "adaptateur-port-hdmi-vers-vga-ab-hdmi-vga-001",
   "description": null,
   "priceHt": "23.900",
   "stock": 50,
   "image": "/uploads/2acbb0d3-aef1-5138-ab69-1b4a204a1613.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "AB-HDMI-VGA-001"
    },
    {
     "name": "EAN",
     "value": "8716309099721"
    }
   ],
   "cat": "adaptateurs-convertisseurs"
  },
  {
   "sku": "A-CF-COMBO15-01",
   "name": "Adaptateur multiport USB-C 15 en 1, gris",
   "slug": "adaptateur-multiport-usb-c-15-en-1-gris-a-cf-combo15-01",
   "description": "Hub USB (2 ports USB 2.0 + 3 ports USB 3.0), USB-C, 2 x HDMI, DisplayPort, PD, lecteur de cartes, LAN Gigabit, audio 3,5 mm",
   "priceHt": "324.900",
   "stock": 2,
   "image": "/uploads/e70b295f-48c8-52b4-a937-57dc786bc3ed.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "A-CF-COMBO15-01"
    },
    {
     "name": "EAN",
     "value": "8716309135900"
    }
   ],
   "cat": "adaptateurs-convertisseurs"
  },
  {
   "sku": "A-CM-COMBO3-02",
   "name": "Adaptateur réseau, USB-C Gigabit, noir",
   "slug": "adaptateur-reseau-usb-c-gigabit-noir-a-cm-combo3-02",
   "description": "3 en 1 : port USB unique, HDMI 4K et USB Type-C PD",
   "priceHt": "34.900",
   "stock": 2,
   "image": "/uploads/598b8064-5a20-5550-85ad-1012340cfacd.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "A-CM-COMBO3-02"
    },
    {
     "name": "EAN",
     "value": "8716309124188"
    }
   ],
   "cat": "adaptateurs-convertisseurs"
  },
  {
   "sku": "A-CM-COMBO6-01",
   "name": "Adaptateur réseau USB-C Gigabit, noir",
   "slug": "adaptateur-reseau-usb-c-gigabit-noir-a-cm-combo6-01",
   "description": "6 en 1 : port USB 3, vidéo HDMI 4K et VGA Full HD, audio stéréo, lecteur de cartes, charge USB-C PD",
   "priceHt": "79.900",
   "stock": 6,
   "image": "/uploads/d976c12c-1184-512a-a6ef-fd930f44391c.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "A-CM-COMBO6-01"
    },
    {
     "name": "EAN",
     "value": "8716309121453"
    }
   ],
   "cat": "adaptateurs-convertisseurs"
  },
  {
   "sku": "A-CM-COMBO9-01",
   "name": "Adaptateur réseau, USB-C Gigabit, noir",
   "slug": "adaptateur-reseau-usb-c-gigabit-noir-a-cm-combo9-01",
   "description": "9 fonctions en 1 : hub USB, vidéo HDMI 4K et VGA Full HD, audio stéréo, port LAN Gigabit",
   "priceHt": "229.900",
   "stock": 5,
   "image": "/uploads/dbcbb25a-11c1-5383-9dab-22d8bd6fb03a.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "A-CM-COMBO9-01"
    },
    {
     "name": "EAN",
     "value": "8716309121439"
    }
   ],
   "cat": "adaptateurs-convertisseurs"
  },
  {
   "sku": "A-CM-COMBO9-02",
   "name": "Adaptateur multiport USB Type-C 9 en 1",
   "slug": "adaptateur-multiport-usb-type-c-9-en-1-a-cm-combo9-02",
   "description": "Hub 3.2 + HDMI + DisplayPort + VGA + PD + LAN + audio stéréo",
   "priceHt": "188.900",
   "stock": 3,
   "image": "/uploads/51d99607-f84e-56c3-9b53-016c8d35389c.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "A-CM-COMBO9-02"
    },
    {
     "name": "EAN",
     "value": "8716309124331"
    }
   ],
   "cat": "adaptateurs-convertisseurs"
  },
  {
   "sku": "A-CM-DPF-01",
   "name": "Adaptateur USB-C > DisplayPort, noir",
   "slug": "adaptateur-usb-c-displayport-noir-a-cm-dpf-01",
   "description": null,
   "priceHt": "38.900",
   "stock": 10,
   "image": "/uploads/abab078c-3b36-5ab8-a976-92dea02f515b.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "A-CM-DPF-01"
    },
    {
     "name": "EAN",
     "value": "8716309097611"
    }
   ],
   "cat": "adaptateurs-convertisseurs"
  },
  {
   "sku": "A-CM-DPM-01",
   "name": "Adaptateur USB-C > DisplayPort, noir",
   "slug": "adaptateur-usb-c-displayport-noir-a-cm-dpm-01",
   "description": "Entrée : USB 3.1 Type-C mâle • Sortie : DisplayPort mâle",
   "priceHt": "24.900",
   "stock": 10,
   "image": "/uploads/202d6848-6e75-5b9d-9161-482fb526fd9f.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "A-CM-DPM-01"
    },
    {
     "name": "EAN",
     "value": "8716309124157"
    }
   ],
   "cat": "adaptateurs-convertisseurs"
  },
  {
   "sku": "A-CM-HDMIF-01",
   "name": "Adaptateur USB-C > HDMI, noir",
   "slug": "adaptateur-usb-c-hdmi-noir-a-cm-hdmif-01",
   "description": null,
   "priceHt": "19.900",
   "stock": 10,
   "image": "/uploads/d63335ad-5627-58b1-b392-7dbc13cc2e28.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "A-CM-HDMIF-01"
    },
    {
     "name": "EAN",
     "value": "8716309097642"
    }
   ],
   "cat": "adaptateurs-convertisseurs"
  },
  {
   "sku": "A-CM-HDMIFDPF-01",
   "name": "Adaptateur USB-C > HDMI + DisplayPort, noir",
   "slug": "adaptateur-usb-c-hdmi-displayport-noir-a-cm-hdmifdpf-01",
   "description": null,
   "priceHt": "69.900",
   "stock": 10,
   "image": "/uploads/6d238532-7ec2-5cde-8416-1b46826c13ca.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "A-CM-HDMIFDPF-01"
    },
    {
     "name": "EAN",
     "value": "8716309137249"
    }
   ],
   "cat": "adaptateurs-convertisseurs"
  },
  {
   "sku": "A-CM-HDMIM-01",
   "name": "Adaptateur USB-C > HDMI, noir",
   "slug": "adaptateur-usb-c-hdmi-noir-a-cm-hdmim-01",
   "description": "Entrée : USB 3.x Type-C mâle • Sortie : HDMI mâle",
   "priceHt": "20.900",
   "stock": 10,
   "image": "/uploads/8055fc57-fe4e-5684-9c37-cd5d61ec2cd2.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "A-CM-HDMIM-01"
    },
    {
     "name": "EAN",
     "value": "8716309124126"
    }
   ],
   "cat": "adaptateurs-convertisseurs"
  },
  {
   "sku": "A-CM-HDMIM-02",
   "name": "Adaptateur USB-C > HDMI, noir",
   "slug": "adaptateur-usb-c-hdmi-noir-a-cm-hdmim-02",
   "description": "Entrée : USB 3.1 Type-C mâle • Sortie : HDMI mâle",
   "priceHt": "34.900",
   "stock": 10,
   "image": "/uploads/42bea823-d876-5648-81bd-e6ffcadce2dd.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "A-CM-HDMIM-02"
    },
    {
     "name": "EAN",
     "value": "8716309124133"
    }
   ],
   "cat": "adaptateurs-convertisseurs"
  },
  {
   "sku": "A-CMU3-LAN-01",
   "name": "Adaptateur réseau USB-C Gigabit, noir",
   "slug": "adaptateur-reseau-usb-c-gigabit-noir-a-cmu3-lan-01",
   "description": "Entrée : USB Type-C mâle • LAN : RJ-45 Gigabit (IEEE 802.3x, 802.1p, 802.1q, 802.3az) • 3 ports USB-A 3.1 Gen 1 (5 Gbit/s)",
   "priceHt": "44.900",
   "stock": 30,
   "image": "/uploads/ed3cd941-d695-5c93-920d-e98493e5bdaa.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "A-CMU3-LAN-01"
    },
    {
     "name": "EAN",
     "value": "8716309120494"
    }
   ],
   "cat": "adaptateurs-convertisseurs"
  },
  {
   "sku": "A-HDMI-FC",
   "name": "Adaptateur HDMI vers mini-HDMI",
   "slug": "adaptateur-hdmi-vers-mini-hdmi-a-hdmi-fc",
   "description": null,
   "priceHt": "6.900",
   "stock": 317,
   "image": "/uploads/b0f07cb8-0663-581c-8f4a-6885f2085c54.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "A-HDMI-FC"
    },
    {
     "name": "EAN",
     "value": "8716309058476"
    }
   ],
   "cat": "adaptateurs-convertisseurs"
  },
  {
   "sku": "A-HDMIM-DPF-02",
   "name": "Adaptateur actif HDMI 4K mâle vers DisplayPort femelle, noir",
   "slug": "adaptateur-actif-hdmi-4k-male-vers-displayport-femelle-noir-a-hdmim-dpf-02",
   "description": null,
   "priceHt": "71.900",
   "stock": 30,
   "image": "/uploads/d3059b7d-b7b1-561c-b3ad-96b363792071.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "A-HDMIM-DPF-02"
    },
    {
     "name": "EAN",
     "value": "8716309124546"
    }
   ],
   "cat": "adaptateurs-convertisseurs"
  },
  {
   "sku": "A-HDMI-VGA-03",
   "name": "Câble adaptateur HDMI vers VGA et audio, port unique, noir",
   "slug": "cable-adaptateur-hdmi-vers-vga-et-audio-port-unique-noir-a-hdmi-vga-03",
   "description": null,
   "priceHt": "12.500",
   "stock": 30,
   "image": "/uploads/2e93f4ca-2dcf-58f8-95ab-566b95077598.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "A-HDMI-VGA-03"
    },
    {
     "name": "EAN",
     "value": "8716309089784"
    }
   ],
   "cat": "adaptateurs-convertisseurs"
  },
  {
   "sku": "A-HDMI-VGA-03-6",
   "name": "Adaptateur port HDMI vers VGA",
   "slug": "adaptateur-port-hdmi-vers-vga-a-hdmi-vga-03-6",
   "description": null,
   "priceHt": "22.900",
   "stock": 30,
   "image": "/uploads/5aa5bb7d-5239-5c8d-b073-571ae4df9e9f.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "A-HDMI-VGA-03-6"
    },
    {
     "name": "EAN",
     "value": "8716309098021"
    }
   ],
   "cat": "adaptateurs-convertisseurs"
  },
  {
   "sku": "A-MHL-002",
   "name": "Câble adaptateur micro-USB vers HDMI (MHL)",
   "slug": "cable-adaptateur-micro-usb-vers-hdmi-mhl-a-mhl-002",
   "description": "Adaptateur HDTV, 5 broches",
   "priceHt": "16.900",
   "stock": 10,
   "image": "/uploads/ce219241-bc97-53c8-bee7-c46a204d7e6e.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "A-MHL-002"
    },
    {
     "name": "EAN",
     "value": "8716309078719"
    }
   ],
   "cat": "adaptateurs-convertisseurs"
  },
  {
   "sku": "A-MHL-003",
   "name": "Câble adaptateur micro-USB vers HDMI (MHL) 11 broches",
   "slug": "cable-adaptateur-micro-usb-vers-hdmi-mhl-11-broches-a-mhl-003",
   "description": null,
   "priceHt": "18.900",
   "stock": 30,
   "image": "/uploads/262257fb-bba5-5ec0-96e3-4c30170294f0.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "A-MHL-003"
    },
    {
     "name": "EAN",
     "value": "8716309078726"
    }
   ],
   "cat": "adaptateurs-convertisseurs"
  },
  {
   "sku": "A-USB2-AMCF-02",
   "name": "Adaptateur USB AM > Type-C, noir",
   "slug": "adaptateur-usb-am-type-c-noir-a-usb2-amcf-02",
   "description": null,
   "priceHt": "3.500",
   "stock": 30,
   "image": "/uploads/d81e3d78-1922-5847-9641-4916ebe99d77.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "A-USB2-AMCF-02"
    },
    {
     "name": "EAN",
     "value": "8716309121088"
    }
   ],
   "cat": "adaptateurs-convertisseurs"
  },
  {
   "sku": "A-USB2-CMAF-01",
   "name": "Adaptateur type-C USB 2.0",
   "slug": "adaptateur-type-c-usb-2-0-a-usb2-cmaf-01",
   "description": "CM/AF",
   "priceHt": "4.500",
   "stock": 30,
   "image": "/uploads/d8199df4-eefa-5d3b-a1f6-39964ac35fff.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "A-USB2-CMAF-01"
    },
    {
     "name": "EAN",
     "value": "8716309093460"
    }
   ],
   "cat": "adaptateurs-convertisseurs"
  },
  {
   "sku": "A-USB3AC-LAN-01",
   "name": "Adaptateur réseau Gigabit USB 3.2",
   "slug": "adaptateur-reseau-gigabit-usb-3-2-a-usb3ac-lan-01",
   "description": "Connecteur combiné USB Type-A + Type-C • Compatible réseaux 10/100/1000 Mbit/s",
   "priceHt": "32.900",
   "stock": 20,
   "image": "/uploads/66e2ad55-437c-58f3-9058-705e6917daeb.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "A-USB3AC-LAN-01"
    },
    {
     "name": "EAN",
     "value": "8716309128186"
    }
   ],
   "cat": "adaptateurs-convertisseurs"
  },
  {
   "sku": "A-USB3AC-LAN2G-01",
   "name": "Adaptateur réseau Gigabit USB 3.2",
   "slug": "adaptateur-reseau-gigabit-usb-3-2-a-usb3ac-lan2g-01",
   "description": "Connecteur combiné USB Type-A + Type-C • Compatible réseaux 10/100/1000 Mbit/s",
   "priceHt": "54.900",
   "stock": 20,
   "image": "/uploads/9f6f8b4f-a048-5cb8-a7c7-e1337fad1c5c.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "A-USB3AC-LAN2G-01"
    },
    {
     "name": "EAN",
     "value": "8716309130424"
    }
   ],
   "cat": "adaptateurs-convertisseurs"
  },
  {
   "sku": "A-USB3C-HDMI-01",
   "name": "Adaptateur USB-C > HDMI, noir",
   "slug": "adaptateur-usb-c-hdmi-noir-a-usb3c-hdmi-01",
   "description": "USB 3.x Type-C mâle • HDMI-A 19 broches femelle",
   "priceHt": "19.900",
   "stock": 10,
   "image": "/uploads/817b1495-e2e5-5922-a5d1-ad53be2e402a.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "A-USB3C-HDMI-01"
    },
    {
     "name": "EAN",
     "value": "8716309121354"
    }
   ],
   "cat": "adaptateurs-convertisseurs"
  },
  {
   "sku": "A-USB3C-HDMIVGA-01",
   "name": "Adaptateur USB-C > HDMI + VGA, noir",
   "slug": "adaptateur-usb-c-hdmi-vga-noir-a-usb3c-hdmivga-01",
   "description": "Interface : USB 3.0 Type-C mâle • HDMI-A 19 broches femelle",
   "priceHt": "44.900",
   "stock": 5,
   "image": "/uploads/91ae65c6-0e1c-5331-b54a-0c6e9b5a34e5.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "A-USB3C-HDMIVGA-01"
    },
    {
     "name": "EAN",
     "value": "8716309121347"
    }
   ],
   "cat": "adaptateurs-convertisseurs"
  },
  {
   "sku": "A-USB3C-LAN-01",
   "name": "Adaptateur réseau USB-C Gigabit",
   "slug": "adaptateur-reseau-usb-c-gigabit-a-usb3c-lan-01",
   "description": "USB Type-C mâle • LAN : RJ-45 femelle Gigabit (IEEE 802.3x, 802.1p, 802.1q, 802.3az)",
   "priceHt": "29.900",
   "stock": 10,
   "image": "/uploads/3ae4669a-13b5-53ec-9b38-45def60ad734.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "A-USB3C-LAN-01"
    },
    {
     "name": "EAN",
     "value": "8716309121385"
    }
   ],
   "cat": "adaptateurs-convertisseurs"
  },
  {
   "sku": "A-USB3-CMAF-01",
   "name": "Adaptateur Type-C USB 3.2 (CM/AF)",
   "slug": "adaptateur-type-c-usb-3-2-cm-af-a-usb3-cmaf-01",
   "description": "USB 3.2 Gen 1, rétrocompatible USB 3.x / 2.0",
   "priceHt": "3.500",
   "stock": 30,
   "image": "/uploads/6656c50e-90ea-5a09-b20f-eaf9d427eba5.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "A-USB3-CMAF-01"
    },
    {
     "name": "EAN",
     "value": "8716309095457"
    }
   ],
   "cat": "adaptateurs-convertisseurs"
  },
  {
   "sku": "A-USB3C-VGA-01",
   "name": "Adaptateur USB-C > VGA, noir",
   "slug": "adaptateur-usb-c-vga-noir-a-usb3c-vga-01",
   "description": "Interface : USB 3.0 Type-C mâle • VGA femelle",
   "priceHt": "38.900",
   "stock": 5,
   "image": "/uploads/9fdbce91-7e98-5168-ac49-e3c981fc909e.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "A-USB3C-VGA-01"
    },
    {
     "name": "EAN",
     "value": "8716309121378"
    }
   ],
   "cat": "adaptateurs-convertisseurs"
  },
  {
   "sku": "A-USB3-HDMI-02",
   "name": "Adaptateur USB > HDMI, noir",
   "slug": "adaptateur-usb-hdmi-noir-a-usb3-hdmi-02",
   "description": "Interface : USB 3.2 Type-A mâle • HDMI-A 19 broches femelle",
   "priceHt": "45.900",
   "stock": 10,
   "image": "/uploads/b6deef1b-bf6b-5481-80a9-84baf82262a3.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "A-USB3-HDMI-02"
    },
    {
     "name": "EAN",
     "value": "8716309099141"
    }
   ],
   "cat": "adaptateurs-convertisseurs"
  },
  {
   "sku": "A-USB3-HDMIVGA-01",
   "name": "Adaptateur USB > HDMI + VGA, noir",
   "slug": "adaptateur-usb-hdmi-vga-noir-a-usb3-hdmivga-01",
   "description": "Interface : USB 3.0 Type-A mâle • HDMI-A 19 broches femelle",
   "priceHt": "54.900",
   "stock": 10,
   "image": "/uploads/1d7cbc52-2f55-5964-8087-c8bd88b7ffbc.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "A-USB3-HDMIVGA-01"
    },
    {
     "name": "EAN",
     "value": "8716309121330"
    }
   ],
   "cat": "adaptateurs-convertisseurs"
  },
  {
   "sku": "DSC-HDMI-VGA-001",
   "name": "Convertisseur HDMI vers VGA",
   "slug": "convertisseur-hdmi-vers-vga-dsc-hdmi-vga-001",
   "description": null,
   "priceHt": "45.900",
   "stock": 10,
   "image": "/uploads/78abb40b-3240-5306-a351-e7913c27f591.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "DSC-HDMI-VGA-001"
    },
    {
     "name": "EAN",
     "value": "8716309077446"
    }
   ],
   "cat": "adaptateurs-convertisseurs"
  },
  {
   "sku": "CCB-HDMIL-5M",
   "name": "Câble HDMI haute vitesse avec Ethernet, 5m « Select Plus Series »",
   "slug": "cable-hdmi-haute-vitesse-avec-ethernet-5m-select-plus-series-ccb-hdmil-5m",
   "description": null,
   "priceHt": "24.900",
   "stock": 40,
   "image": "/uploads/8549e620-2753-5506-a9fa-b79d3e07adf8.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "CCB-HDMIL-5M"
    },
    {
     "name": "EAN",
     "value": "8716309107655"
    }
   ],
   "cat": "cables-video"
  },
  {
   "sku": "CC-DP2-10M",
   "name": "Câble DisplayPort 4K, 10 m",
   "slug": "cable-displayport-4k-10-m-cc-dp2-10m",
   "description": "DisplayPort 1.2 • 2 x DisplayPort mâle",
   "priceHt": "46.900",
   "stock": 30,
   "image": "/uploads/81c91e9d-6d41-5f75-a281-9e6a16dd92b7.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "CC-DP2-10M"
    },
    {
     "name": "EAN",
     "value": "8716309120609"
    }
   ],
   "cat": "cables-video"
  },
  {
   "sku": "CC-DP2-5M",
   "name": "Câble DisplayPort 4K, 5 m",
   "slug": "cable-displayport-4k-5-m-cc-dp2-5m",
   "description": "DisplayPort 1.2 • 2 x DisplayPort mâle",
   "priceHt": "29.900",
   "stock": 40,
   "image": "/uploads/bbcbfacd-f9ba-541a-a04f-48819de7f75b.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "CC-DP2-5M"
    },
    {
     "name": "EAN",
     "value": "8716309120593"
    }
   ],
   "cat": "cables-video"
  },
  {
   "sku": "CC-DP-HDMI-3M",
   "name": "Câble DisplayPort 4K vers HDMI, 3 m",
   "slug": "cable-displayport-4k-vers-hdmi-3-m-cc-dp-hdmi-3m",
   "description": "DP 1.2 • Entrée DisplayPort mâle / sortie HDMI Type-A mâle, connecteurs plaqués or",
   "priceHt": "19.900",
   "stock": 50,
   "image": "/uploads/98051912-447e-505b-ac4e-7312fd419c95.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "CC-DP-HDMI-3M"
    },
    {
     "name": "EAN",
     "value": "8716309080002"
    }
   ],
   "cat": "cables-video"
  },
  {
   "sku": "CC-DP-HDMI-5M",
   "name": "Câble DisplayPort 4K vers HDMI, 5 m",
   "slug": "cable-displayport-4k-vers-hdmi-5-m-cc-dp-hdmi-5m",
   "description": "DP 1.2 • Entrée DisplayPort mâle / sortie HDMI Type-A mâle, plaqués or • 3840 x 2160 @ 60 Hz",
   "priceHt": "48.900",
   "stock": 9,
   "image": "/uploads/60f0c0b4-3cdb-5ea0-b023-d93711741b5a.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "CC-DP-HDMI-5M"
    },
    {
     "name": "EAN",
     "value": "8716309098090"
    }
   ],
   "cat": "cables-video"
  },
  {
   "sku": "CC-HDMI4-10M",
   "name": "Câble HDMI haute vitesse avec Ethernet, 10 m",
   "slug": "cable-hdmi-haute-vitesse-avec-ethernet-10-m-cc-hdmi4-10m",
   "description": "Compatible HDMI 2.0 et versions antérieures • 2 x HDMI Type-A mâle, plaqués or",
   "priceHt": "39.900",
   "stock": 15,
   "image": "/uploads/369d6ded-5110-51cf-9c0e-b85ace542bd1.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "CC-HDMI4-10M"
    },
    {
     "name": "EAN",
     "value": "8716309065856"
    }
   ],
   "cat": "cables-video"
  },
  {
   "sku": "CC-HDMI4-30M",
   "name": "Câble HDMI haute vitesse avec Ethernet, 30 m",
   "slug": "cable-hdmi-haute-vitesse-avec-ethernet-30-m-cc-hdmi4-30m",
   "description": null,
   "priceHt": "235.900",
   "stock": 15,
   "image": "/uploads/3b00a4e5-e311-5c3c-a36a-f65381843dac.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "CC-HDMI4-30M"
    },
    {
     "name": "EAN",
     "value": "8716309074339"
    }
   ],
   "cat": "cables-video"
  },
  {
   "sku": "DSP-2PH4-04",
   "name": "Câble HDMI passif à double port",
   "slug": "cable-hdmi-passif-a-double-port-dsp-2ph4-04",
   "description": null,
   "priceHt": "11.900",
   "stock": 50,
   "image": "/uploads/1f633750-3181-5f2c-9ca9-f42dbe2b490d.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "DSP-2PH4-04"
    },
    {
     "name": "EAN",
     "value": "8716309096669"
    }
   ],
   "cat": "cables-video"
  },
  {
   "sku": "EE2-U3S-6",
   "name": "Boîtier pour disque dur 2,5\" USB 3.0, noir",
   "slug": "boitier-pour-disque-dur-2-5-usb-3-0-noir-ee2-u3s-6",
   "description": null,
   "priceHt": "32.900",
   "stock": 30,
   "image": "/uploads/b019d67d-5b33-5a82-b6da-9d4dc09d61f7.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "EE2-U3S-6"
    },
    {
     "name": "EAN",
     "value": "8716309113526"
    }
   ],
   "cat": "hubs-usb-cartes-acquisition"
  },
  {
   "sku": "EE2-U3S-6-GR",
   "name": "Boîtier pour disque dur 2,5\" USB 3.0, gris",
   "slug": "boitier-pour-disque-dur-2-5-usb-3-0-gris-ee2-u3s-6-gr",
   "description": null,
   "priceHt": "32.900",
   "stock": 30,
   "image": "/uploads/0bd57719-c4b3-5f03-bdbb-e67e8ebbde77.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "EE2-U3S-6-GR"
    },
    {
     "name": "EAN",
     "value": "8716309114271"
    }
   ],
   "cat": "hubs-usb-cartes-acquisition"
  },
  {
   "sku": "SC-USB-02",
   "name": "Carte son USB « Virtus2 »",
   "slug": "carte-son-usb-virtus2-sc-usb-02",
   "description": null,
   "priceHt": "12.900",
   "stock": 10,
   "image": "/uploads/b924b9ff-4588-552e-9051-989f89857ab4.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "SC-USB-02"
    },
    {
     "name": "EAN",
     "value": "8716309137539"
    }
   ],
   "cat": "hubs-usb-cartes-acquisition"
  },
  {
   "sku": "UHB-ACM-U3P1U2P6-01",
   "name": "Hub USB Type-C, 4 ports USB 3.1 Gen 1, argent",
   "slug": "hub-usb-type-c-4-ports-usb-3-1-gen-1-argent-uhb-acm-u3p1u2p6-01",
   "description": null,
   "priceHt": "19.900",
   "stock": 70,
   "image": "/uploads/01fe6a3e-4a26-5236-9c7a-fa3ac3ec1bd8.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "UHB-ACM-U3P1U2P6-01"
    },
    {
     "name": "EAN",
     "value": "8716309138222"
    }
   ],
   "cat": "hubs-usb-cartes-acquisition"
  },
  {
   "sku": "UHB-CM-CRU3P1U2P2-01",
   "name": "Hub USB Type-C, 3 ports USB 3.0 + lecteur de cartes",
   "slug": "hub-usb-type-c-3-ports-usb-3-0-lecteur-de-cartes-uhb-cm-cru3p1u2p2-01",
   "description": null,
   "priceHt": "26.900",
   "stock": 100,
   "image": "/uploads/be0a20cd-dd74-5867-8dd4-230ca2f460a9.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "UHB-CM-CRU3P1U2P2-01"
    },
    {
     "name": "EAN",
     "value": "8716309124232"
    }
   ],
   "cat": "hubs-usb-cartes-acquisition"
  },
  {
   "sku": "UHB-CM-U310-A2C2-01",
   "name": "Hub USB 3.2 Gen 2 Type-C, 4 ports A + C",
   "slug": "hub-usb-3-2-gen-2-type-c-4-ports-a-c-uhb-cm-u310-a2c2-01",
   "description": "Aluminium + ABS • Rétrocompatible USB 3.x / 2.0",
   "priceHt": "49.900",
   "stock": 430,
   "image": "/uploads/b49cd620-6de1-5f84-989d-c47cc7bfe8a4.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "UHB-CM-U310-A2C2-01"
    },
    {
     "name": "EAN",
     "value": "8716309137232"
    }
   ],
   "cat": "hubs-usb-cartes-acquisition"
  },
  {
   "sku": "UHB-CM-U310-A4-01",
   "name": "Hub USB 3.2 Gen 2 Type-C, 4 ports",
   "slug": "hub-usb-3-2-gen-2-type-c-4-ports-uhb-cm-u310-a4-01",
   "description": null,
   "priceHt": "54.900",
   "stock": 470,
   "image": "/uploads/2a4db511-f32b-5e92-9780-53f6299ee8a8.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "UHB-CM-U310-A4-01"
    },
    {
     "name": "EAN",
     "value": "8716309137218"
    }
   ],
   "cat": "hubs-usb-cartes-acquisition"
  },
  {
   "sku": "UHB-CM-U3P1U2P3-01",
   "name": "Hub USB Type-C 4 ports",
   "slug": "hub-usb-type-c-4-ports-uhb-cm-u3p1u2p3-01",
   "description": "1 x USB 3.1 + 3 x USB 2.0",
   "priceHt": "59.900",
   "stock": 100,
   "image": "/uploads/ae6a8c70-c505-530d-90a4-3930761070ea.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "UHB-CM-U3P1U2P3-01"
    },
    {
     "name": "EAN",
     "value": "8716309124225"
    }
   ],
   "cat": "hubs-usb-cartes-acquisition"
  },
  {
   "sku": "UHB-CM-U3P1U2P3-03",
   "name": "Hub USB Type-C, 4 ports USB 3.1 Gen 1, argent",
   "slug": "hub-usb-type-c-4-ports-usb-3-1-gen-1-argent-uhb-cm-u3p1u2p3-03",
   "description": null,
   "priceHt": "12.900",
   "stock": 100,
   "image": "/uploads/9a9f0ec9-bb7d-5f2f-b84b-8550009da5a2.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "UHB-CM-U3P1U2P3-03"
    },
    {
     "name": "EAN",
     "value": "8716309130851"
    }
   ],
   "cat": "hubs-usb-cartes-acquisition"
  },
  {
   "sku": "UHB-CM-U3P4-02",
   "name": "Hub USB 3.1 Gen 1 Type-C, 4 ports",
   "slug": "hub-usb-3-1-gen-1-type-c-4-ports-uhb-cm-u3p4-02",
   "description": null,
   "priceHt": "22.900",
   "stock": 80,
   "image": "/uploads/5fdab3f5-9fe2-5a16-8946-b0957917c425.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "UHB-CM-U3P4-02"
    },
    {
     "name": "EAN",
     "value": "8716309129718"
    }
   ],
   "cat": "hubs-usb-cartes-acquisition"
  },
  {
   "sku": "UHB-CR3-01",
   "name": "Lecteur de cartes mémoire SD compact, USB 3.0",
   "slug": "lecteur-de-cartes-memoire-sd-compact-usb-3-0-uhb-cr3-01",
   "description": null,
   "priceHt": "18.900",
   "stock": 20,
   "image": "/uploads/60e789f0-ff60-5391-9d8f-6a0bb802f049.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "UHB-CR3-01"
    },
    {
     "name": "EAN",
     "value": "8716309087704"
    }
   ],
   "cat": "hubs-usb-cartes-acquisition"
  },
  {
   "sku": "UHB-U2P10P-01",
   "name": "Hub USB 2.0, 10 ports",
   "slug": "hub-usb-2-0-10-ports-uhb-u2p10p-01",
   "description": "USB 2.0 (compatible 1.1) • 480 Mbit/s",
   "priceHt": "33.900",
   "stock": 50,
   "image": "/uploads/03818a91-304c-5871-a56b-24858a3f4a20.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "UHB-U2P10P-01"
    },
    {
     "name": "EAN",
     "value": "8716309124638"
    }
   ],
   "cat": "hubs-usb-cartes-acquisition"
  },
  {
   "sku": "UHB-U2P4-04",
   "name": "Hub USB 2.0, 4 ports",
   "slug": "hub-usb-2-0-4-ports-uhb-u2p4-04",
   "description": "Haute vitesse, 480 Mbit/s max.",
   "priceHt": "13.900",
   "stock": 100,
   "image": "/uploads/ddcde57b-74c4-5f0b-87bb-f1b37916bf6f.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "UHB-U2P4-04"
    },
    {
     "name": "EAN",
     "value": "8716309117647"
    }
   ],
   "cat": "hubs-usb-cartes-acquisition"
  },
  {
   "sku": "UHB-U3P4-03",
   "name": "Concentrateur USB 3.1 Gen 1, 4 ports",
   "slug": "concentrateur-usb-3-1-gen-1-4-ports-uhb-u3p4-03",
   "description": null,
   "priceHt": "23.900",
   "stock": 50,
   "image": "/uploads/3d40624f-efec-59d4-884c-b937a93493e4.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "UHB-U3P4-03"
    },
    {
     "name": "EAN",
     "value": "8716309117012"
    }
   ],
   "cat": "hubs-usb-cartes-acquisition"
  },
  {
   "sku": "UHB-U3P4P-02",
   "name": "Hub USB 3.2 Gen 1, 4 ports",
   "slug": "hub-usb-3-2-gen-1-4-ports-uhb-u3p4p-02",
   "description": null,
   "priceHt": "29.900",
   "stock": 131,
   "image": "/uploads/94e6baf5-f8d4-5b6b-bf39-e32766170c40.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "UHB-U3P4P-02"
    },
    {
     "name": "EAN",
     "value": "8716309128858"
    }
   ],
   "cat": "hubs-usb-cartes-acquisition"
  },
  {
   "sku": "UHG-4K2-01",
   "name": "Boîtier d'acquisition vidéo HDMI USB 4K",
   "slug": "boitier-d-acquisition-video-hdmi-usb-4k-uhg-4k2-01",
   "description": "Avec sortie HDMI (pass-through)",
   "priceHt": "88.900",
   "stock": 30,
   "image": "/uploads/69755ce6-0294-5cf9-94c5-1967669e8da8.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "UHG-4K2-01"
    },
    {
     "name": "EAN",
     "value": "8716309120838"
    }
   ],
   "cat": "hubs-usb-cartes-acquisition"
  },
  {
   "sku": "UPC-U3",
   "name": "Carte PCI-Express vers USB 3.2 Gen 1 (clé A)",
   "slug": "carte-pci-express-vers-usb-3-2-gen-1-cle-a-upc-u3",
   "description": "Convertit un slot PCI-E x1 en connecteur interne USB 3.2 Gen 1 Type-E",
   "priceHt": "56.900",
   "stock": 30,
   "image": "/uploads/401963d2-edbe-5c61-9343-260d63984a4d.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "UPC-U3"
    },
    {
     "name": "EAN",
     "value": "8716309138192"
    }
   ],
   "cat": "hubs-usb-cartes-acquisition"
  },
  {
   "sku": "UVG-002",
   "name": "Appareil de capture vidéo USB",
   "slug": "appareil-de-capture-video-usb-uvg-002",
   "description": null,
   "priceHt": "46.900",
   "stock": 5,
   "image": "/uploads/d13b6079-7fb0-52ae-a05f-5faafb130ec7.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "UVG-002"
    },
    {
     "name": "EAN",
     "value": "8716309079983"
    }
   ],
   "cat": "hubs-usb-cartes-acquisition"
  },
  {
   "sku": "WNP-UA1300-03",
   "name": "Adaptateur Wi-Fi USB AC1300 double bande compact",
   "slug": "adaptateur-wi-fi-usb-ac1300-double-bande-compact-wnp-ua1300-03",
   "description": null,
   "priceHt": "44.900",
   "stock": 100,
   "image": "/uploads/fc9031dd-569a-55af-b4f9-b15eed4fb100.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "WNP-UA1300-03"
    },
    {
     "name": "EAN",
     "value": "8716309129435"
    }
   ],
   "cat": "hubs-usb-cartes-acquisition"
  },
  {
   "sku": "CM-55ST-01",
   "name": "Support plafond TV orientable, 23\"-55\"",
   "slug": "support-plafond-tv-orientable-23-55-cm-55st-01",
   "description": null,
   "priceHt": "59.900",
   "stock": 24,
   "image": "/uploads/ea2007a7-3876-5304-8761-e5bf0cdf3824.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "CM-55ST-01"
    },
    {
     "name": "EAN",
     "value": "8716309135184"
    }
   ],
   "cat": "supports-tv-meubles-projecteurs"
  },
  {
   "sku": "CM-70ST-01",
   "name": "Support plafond TV orientable, 32\"-70\"",
   "slug": "support-plafond-tv-orientable-32-70-cm-70st-01",
   "description": null,
   "priceHt": "124.900",
   "stock": 12,
   "image": "/uploads/2f54b74c-37dd-5bb1-8b14-286d3d1599f2.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "CM-70ST-01"
    },
    {
     "name": "EAN",
     "value": "8716309128087"
    }
   ],
   "cat": "supports-tv-meubles-projecteurs"
  },
  {
   "sku": "TVS-55T-02",
   "name": "Meuble TV, 32\"-55\", noir",
   "slug": "meuble-tv-32-55-noir-tvs-55t-02",
   "description": null,
   "priceHt": "299.900",
   "stock": 2,
   "image": "/uploads/2853809e-a64d-5ab8-89d8-c12b76166f82.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "TVS-55T-02"
    },
    {
     "name": "EAN",
     "value": "8716309135092"
    }
   ],
   "cat": "supports-tv-meubles-projecteurs"
  },
  {
   "sku": "TVS-55T-03",
   "name": "Meuble TV à roulettes (inclinable), 32\"-55\", noir",
   "slug": "meuble-tv-a-roulettes-inclinable-32-55-noir-tvs-55t-03",
   "description": null,
   "priceHt": "189.900",
   "stock": 25,
   "image": "/uploads/ccd0be40-de58-50e3-a0c7-530ac8776846.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "TVS-55T-03"
    },
    {
     "name": "EAN",
     "value": "8716309135467"
    }
   ],
   "cat": "supports-tv-meubles-projecteurs"
  },
  {
   "sku": "TVS-55T-03-W",
   "name": "Meuble TV à roulettes (inclinable), 32\"-55\", blanc",
   "slug": "meuble-tv-a-roulettes-inclinable-32-55-blanc-tvs-55t-03-w",
   "description": null,
   "priceHt": "189.900",
   "stock": 25,
   "image": "/uploads/1de245d8-6502-500e-8006-46e7a5d86eee.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "TVS-55T-03-W"
    },
    {
     "name": "EAN",
     "value": "8716309135511"
    }
   ],
   "cat": "supports-tv-meubles-projecteurs"
  },
  {
   "sku": "TVS-70T-02",
   "name": "Support TV sur pied en aluminium avec roulettes, 37\"-70\", noir",
   "slug": "support-tv-sur-pied-en-aluminium-avec-roulettes-37-70-noir-tvs-70t-02",
   "description": null,
   "priceHt": "744.900",
   "stock": 35,
   "image": "/uploads/36d009cd-4486-5615-b463-7e89b9b2762d.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "TVS-70T-02"
    },
    {
     "name": "EAN",
     "value": "8716309129008"
    }
   ],
   "cat": "supports-tv-meubles-projecteurs"
  },
  {
   "sku": "TVS-70T-02-W",
   "name": "Support de sol TV en aluminium avec roulettes, 37\"-70\", blanc",
   "slug": "support-de-sol-tv-en-aluminium-avec-roulettes-37-70-blanc-tvs-70t-02-w",
   "description": null,
   "priceHt": "744.900",
   "stock": 9,
   "image": "/uploads/965a1650-83ae-5c2c-8b14-43cc73bdeeee.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "TVS-70T-02-W"
    },
    {
     "name": "EAN",
     "value": "8716309128995"
    }
   ],
   "cat": "supports-tv-meubles-projecteurs"
  },
  {
   "sku": "TVS-75S-02",
   "name": "Meuble TV à roulettes, 37\"-75\", noir",
   "slug": "meuble-tv-a-roulettes-37-75-noir-tvs-75s-02",
   "description": null,
   "priceHt": "259.900",
   "stock": 10,
   "image": "/uploads/79558ac4-4e41-57ef-8b97-8afa92b3eb73.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "TVS-75S-02"
    },
    {
     "name": "EAN",
     "value": "8716309135054"
    }
   ],
   "cat": "supports-tv-meubles-projecteurs"
  },
  {
   "sku": "TVS-86ST-01",
   "name": "Meuble TV, 43\"-86\", noir",
   "slug": "meuble-tv-43-86-noir-tvs-86st-01",
   "description": null,
   "priceHt": "369.900",
   "stock": 2,
   "image": "/uploads/ecf055e9-7339-5351-b1d0-a979515143da.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "TVS-86ST-01"
    },
    {
     "name": "EAN",
     "value": "8716309135108"
    }
   ],
   "cat": "supports-tv-meubles-projecteurs"
  },
  {
   "sku": "TVS-86TR-01-W",
   "name": "Meuble TV métal à roulettes, 55\"-86\", blanc",
   "slug": "meuble-tv-metal-a-roulettes-55-86-blanc-tvs-86tr-01-w",
   "description": "Hauteur réglable • Étagère pour équipement • Roues pivotantes avec blocage",
   "priceHt": "349.900",
   "stock": 5,
   "image": "/uploads/b66d353a-6c14-5504-a5d4-dad5bb191792.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "TVS-86TR-01-W"
    },
    {
     "name": "EAN",
     "value": "8716309135047"
    }
   ],
   "cat": "supports-tv-meubles-projecteurs"
  },
  {
   "sku": "TVS-D65S-01",
   "name": "Meuble TV de table (pivotant), 32\"-65\"",
   "slug": "meuble-tv-de-table-pivotant-32-65-tvs-d65s-01",
   "description": null,
   "priceHt": "72.900",
   "stock": 20,
   "image": "/uploads/64fac6cf-8a72-55eb-aac3-e380ec7379b8.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "TVS-D65S-01"
    },
    {
     "name": "EAN",
     "value": "8716309135061"
    }
   ],
   "cat": "supports-tv-meubles-projecteurs"
  },
  {
   "sku": "WM-42F-04",
   "name": "Support mural TV fixe, 23\"-42\"",
   "slug": "support-mural-tv-fixe-23-42-wm-42f-04",
   "description": null,
   "priceHt": "12.900",
   "stock": 26,
   "image": "/uploads/22ff67cf-b15f-59f4-9763-67c74403ff1d.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "WM-42F-04"
    },
    {
     "name": "EAN",
     "value": "8716309127998"
    }
   ],
   "cat": "supports-tv-meubles-projecteurs"
  },
  {
   "sku": "WM-55F-04",
   "name": "Support mural TV fixe, 32\"-55\"",
   "slug": "support-mural-tv-fixe-32-55-wm-55f-04",
   "description": null,
   "priceHt": "19.900",
   "stock": 32,
   "image": "/uploads/e4f0446d-a24e-5a47-9a1b-163c09afdb77.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "WM-55F-04"
    },
    {
     "name": "EAN",
     "value": "8716309128018"
    }
   ],
   "cat": "supports-tv-meubles-projecteurs"
  },
  {
   "sku": "WM-70F-02",
   "name": "Support mural TV, 37\"-70\"",
   "slug": "support-mural-tv-37-70-wm-70f-02",
   "description": null,
   "priceHt": "24.900",
   "stock": 16,
   "image": "/uploads/4ace3cfe-79f6-508b-bc0f-827c04ed8dbe.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "WM-70F-02"
    },
    {
     "name": "EAN",
     "value": "8716309127936"
    }
   ],
   "cat": "supports-tv-meubles-projecteurs"
  },
  {
   "sku": "WM-70F-03",
   "name": "Support mural TV, 37\"-70\"",
   "slug": "support-mural-tv-37-70-wm-70f-03",
   "description": null,
   "priceHt": "24.900",
   "stock": 28,
   "image": "/uploads/fe50e45e-af64-533f-8f66-100c6be2f3c4.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "WM-70F-03"
    },
    {
     "name": "EAN",
     "value": "8716309128032"
    }
   ],
   "cat": "supports-tv-meubles-projecteurs"
  },
  {
   "sku": "WM-80ST-02",
   "name": "Support mural TV orientable, 37\"-80\"",
   "slug": "support-mural-tv-orientable-37-80-wm-80st-02",
   "description": null,
   "priceHt": "128.900",
   "stock": 15,
   "image": "/uploads/231cdde4-2f74-5f09-8833-ce515c267b29.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "WM-80ST-02"
    },
    {
     "name": "EAN",
     "value": "8716309126229"
    }
   ],
   "cat": "supports-tv-meubles-projecteurs"
  },
  {
   "sku": "WM-80ST-05",
   "name": "Support mural pour écran LCD / Plasma, 37\"-80\"",
   "slug": "support-mural-pour-ecran-lcd-plasma-37-80-wm-80st-05",
   "description": null,
   "priceHt": "132.900",
   "stock": 10,
   "image": "/uploads/a035dee5-c52a-5ca4-a285-fa4141a89138.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "WM-80ST-05"
    },
    {
     "name": "EAN",
     "value": "8716309127806"
    }
   ],
   "cat": "supports-tv-meubles-projecteurs"
  },
  {
   "sku": "WM-90F-01",
   "name": "Support mural TV, 43\"-90\"",
   "slug": "support-mural-tv-43-90-wm-90f-01",
   "description": null,
   "priceHt": "59.900",
   "stock": 12,
   "image": "/uploads/d9fd89bb-ca17-5265-81c0-5764996e7ca1.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "WM-90F-01"
    },
    {
     "name": "EAN",
     "value": "8716309126175"
    }
   ],
   "cat": "supports-tv-meubles-projecteurs"
  },
  {
   "sku": "WM-KIT-01",
   "name": "Kit de vis de fixation murale universelle",
   "slug": "kit-de-vis-de-fixation-murale-universelle-wm-kit-01",
   "description": null,
   "priceHt": "15.900",
   "stock": 10,
   "image": "/uploads/ac65d893-dd9e-5a20-a2bd-7023379bcb5a.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "WM-KIT-01"
    },
    {
     "name": "EAN",
     "value": "8716309135498"
    }
   ],
   "cat": "supports-tv-meubles-projecteurs"
  },
  {
   "sku": "MA-D1-01",
   "name": "Bras d'écran de bureau réglable, 17\"-32\"",
   "slug": "bras-d-ecran-de-bureau-reglable-17-32-ma-d1-01",
   "description": "Jusqu'à 9 kg",
   "priceHt": "64.900",
   "stock": 6,
   "image": "/uploads/222dea33-e0c4-5c5f-8af4-e6426e71568c.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "MA-D1-01"
    },
    {
     "name": "EAN",
     "value": "8716309126069"
    }
   ],
   "cat": "bras-supports-d-ecran-pc-portable"
  },
  {
   "sku": "MA-D1-02",
   "name": "Bras pour écran simple monté sur bureau, 17\"-32\"",
   "slug": "bras-pour-ecran-simple-monte-sur-bureau-17-32-ma-d1-02",
   "description": null,
   "priceHt": "48.900",
   "stock": 6,
   "image": "/uploads/440405d2-15d4-5b0d-8245-e20cdb370920.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "MA-D1-02"
    },
    {
     "name": "EAN",
     "value": "8716309127561"
    }
   ],
   "cat": "bras-supports-d-ecran-pc-portable"
  },
  {
   "sku": "MA-D1-03",
   "name": "Bras pour écran simple monté sur bureau, 17\"-32\"",
   "slug": "bras-pour-ecran-simple-monte-sur-bureau-17-32-ma-d1-03",
   "description": null,
   "priceHt": "54.900",
   "stock": 6,
   "image": "/uploads/76a32353-8432-5617-9bd8-94659b0ed4c5.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "MA-D1-03"
    },
    {
     "name": "EAN",
     "value": "8716309127585"
    }
   ],
   "cat": "bras-supports-d-ecran-pc-portable"
  },
  {
   "sku": "MA-D2-02",
   "name": "Support 2 écrans ajustable (inclinable), 17\"-32\"",
   "slug": "support-2-ecrans-ajustable-inclinable-17-32-ma-d2-02",
   "description": "Jusqu'à 9 kg",
   "priceHt": "78.900",
   "stock": 4,
   "image": "/uploads/11a36de9-168e-5412-ab2d-a04a9a54c990.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "MA-D2-02"
    },
    {
     "name": "EAN",
     "value": "8716309127592"
    }
   ],
   "cat": "bras-supports-d-ecran-pc-portable"
  },
  {
   "sku": "MA-D2-03",
   "name": "Bras pour écran double réglable monté sur bureau, 17\"-32\"",
   "slug": "bras-pour-ecran-double-reglable-monte-sur-bureau-17-32-ma-d2-03",
   "description": null,
   "priceHt": "92.500",
   "stock": 20,
   "image": "/uploads/8cef5b1e-a5d4-592a-9f43-3ea800d8f176.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "MA-D2-03"
    },
    {
     "name": "EAN",
     "value": "8716309128155"
    }
   ],
   "cat": "bras-supports-d-ecran-pc-portable"
  },
  {
   "sku": "MA-D4-01",
   "name": "Bras de montage réglable pour 4 écrans, 13\"-32\"",
   "slug": "bras-de-montage-reglable-pour-4-ecrans-13-32-ma-d4-01",
   "description": "Rotation, inclinaison, pivotement",
   "priceHt": "129.900",
   "stock": 6,
   "image": "/uploads/6a9223ac-2ab5-5fdf-b188-a7f9b7662c08.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "MA-D4-01"
    },
    {
     "name": "EAN",
     "value": "8716309135399"
    }
   ],
   "cat": "bras-supports-d-ecran-pc-portable"
  },
  {
   "sku": "MA-DA1-05",
   "name": "Support d'écran, 17\"-32\"",
   "slug": "support-d-ecran-17-32-ma-da1-05",
   "description": "Jusqu'à 9 kg",
   "priceHt": "89.900",
   "stock": 12,
   "image": "/uploads/b3a66828-e5ed-5e34-b042-e05d7df1611c.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "MA-DA1-05"
    },
    {
     "name": "EAN",
     "value": "8716309127707"
    }
   ],
   "cat": "bras-supports-d-ecran-pc-portable"
  },
  {
   "sku": "MA-DA2-05",
   "name": "Bras double écran réglable de bureau, gris sidéral",
   "slug": "bras-double-ecran-reglable-de-bureau-gris-sideral-ma-da2-05",
   "description": null,
   "priceHt": "158.900",
   "stock": 12,
   "image": "/uploads/832ed756-f264-52c9-828a-b3ba3dca1397.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "MA-DA2-05"
    },
    {
     "name": "EAN",
     "value": "8716309127714"
    }
   ],
   "cat": "bras-supports-d-ecran-pc-portable"
  },
  {
   "sku": "MA-WA1-01",
   "name": "Bras mural réglable pour écran, jusqu'à 27\"",
   "slug": "bras-mural-reglable-pour-ecran-jusqu-a-27-ma-wa1-01",
   "description": "Jusqu'à 7 kg",
   "priceHt": "84.900",
   "stock": 20,
   "image": "/uploads/3dbac15b-c76f-5465-b307-d035a7425de0.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "MA-WA1-01"
    },
    {
     "name": "EAN",
     "value": "8716309106887"
    }
   ],
   "cat": "bras-supports-d-ecran-pc-portable"
  },
  {
   "sku": "MA-WA2-02",
   "name": "Bras mural réglable pour 2 écrans, 17\"-32\"",
   "slug": "bras-mural-reglable-pour-2-ecrans-17-32-ma-wa2-02",
   "description": "Jusqu'à 8 kg",
   "priceHt": "248.900",
   "stock": 1,
   "image": "/uploads/7b449dfb-b25c-5e1e-b55e-1835db1cd18b.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "MA-WA2-02"
    },
    {
     "name": "EAN",
     "value": "8716309127660"
    }
   ],
   "cat": "bras-supports-d-ecran-pc-portable"
  },
  {
   "sku": "MA-WA3-01",
   "name": "Bras mural réglable pour 3 moniteurs",
   "slug": "bras-mural-reglable-pour-3-moniteurs-ma-wa3-01",
   "description": null,
   "priceHt": "288.900",
   "stock": 2,
   "image": "/uploads/bcd58e14-c19c-57cd-9f11-6afc550eb36f.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "MA-WA3-01"
    },
    {
     "name": "EAN",
     "value": "8716309127677"
    }
   ],
   "cat": "bras-supports-d-ecran-pc-portable"
  },
  {
   "sku": "NBS-D1-03",
   "name": "Support pliable pour ordinateur portable, argenté",
   "slug": "support-pliable-pour-ordinateur-portable-argente-nbs-d1-03",
   "description": null,
   "priceHt": "84.900",
   "stock": 20,
   "image": "/uploads/050142c5-8d16-5bc7-9697-3c4bead4fbfc.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "NBS-D1-03"
    },
    {
     "name": "EAN",
     "value": "8716309127752"
    }
   ],
   "cat": "bras-supports-d-ecran-pc-portable"
  },
  {
   "sku": "NBS-D1-04",
   "name": "Support pliable pour ordinateur portable, gris sidéral",
   "slug": "support-pliable-pour-ordinateur-portable-gris-sideral-nbs-d1-04",
   "description": null,
   "priceHt": "39.900",
   "stock": 269,
   "image": "/uploads/37cd17f1-2a0d-52d0-9db8-5ad0fe32f3dc.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "NBS-D1-04"
    },
    {
     "name": "EAN",
     "value": "8716309132329"
    }
   ],
   "cat": "bras-supports-d-ecran-pc-portable"
  },
  {
   "sku": "NBS-DR1-01",
   "name": "Support rotatif pour ordinateur portable, gris sidéral",
   "slug": "support-rotatif-pour-ordinateur-portable-gris-sideral-nbs-dr1-01",
   "description": null,
   "priceHt": "48.900",
   "stock": 150,
   "image": "/uploads/e09d75f9-5a45-5f9a-83ae-e9f33560f0cc.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "NBS-DR1-01"
    },
    {
     "name": "EAN",
     "value": "8716309132336"
    }
   ],
   "cat": "bras-supports-d-ecran-pc-portable"
  },
  {
   "sku": "MIC-205",
   "name": "Microphone de bureau",
   "slug": "microphone-de-bureau-mic-205",
   "description": null,
   "priceHt": "14.900",
   "stock": 10,
   "image": "/uploads/cab34b20-c6d9-5652-8221-276e06a16bda.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "MIC-205"
    },
    {
     "name": "EAN",
     "value": "8716309079914"
    }
   ],
   "cat": "souris-microphones"
  },
  {
   "sku": "MIC-D-02",
   "name": "Microphone de bureau, noir",
   "slug": "microphone-de-bureau-noir-mic-d-02",
   "description": "Fiche audio 3,5 mm • 100 Hz - 16 kHz • Sensibilité -62 ±3 dB",
   "priceHt": "12.900",
   "stock": 10,
   "image": "/uploads/a0af8930-914d-54c2-827b-b6c2b4115fb7.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "MIC-D-02"
    },
    {
     "name": "EAN",
     "value": "8716309100557"
    }
   ],
   "cat": "souris-microphones"
  },
  {
   "sku": "MIC-D-04",
   "name": "Microphone de bureau, noir",
   "slug": "microphone-de-bureau-noir-mic-d-04",
   "description": "Jack 3,5 mm TRS • Micro à condensateur • 20 Hz - 20 kHz • Sensibilité -58 ±3 dB",
   "priceHt": "17.900",
   "stock": 10,
   "image": "/uploads/f110e631-c371-501f-83ad-f0db0d004005.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "MIC-D-04"
    },
    {
     "name": "EAN",
     "value": "8716309117197"
    }
   ],
   "cat": "souris-microphones"
  },
  {
   "sku": "MIC-DU-01",
   "name": "Microphone de bureau USB avec carte son intégrée",
   "slug": "microphone-de-bureau-usb-avec-carte-son-integree-mic-du-01",
   "description": "USB 2.0 • Condensateur omnidirectionnel • -40 ±3 dB",
   "priceHt": "32.900",
   "stock": 10,
   "image": "/uploads/88cdb1ec-a120-5591-8173-13319d2515f0.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "MIC-DU-01"
    },
    {
     "name": "EAN",
     "value": "8716309117371"
    }
   ],
   "cat": "souris-microphones"
  },
  {
   "sku": "MUS-ERGO-01",
   "name": "Souris optique filaire ergonomique 6 boutons, noire",
   "slug": "souris-optique-filaire-ergonomique-6-boutons-noire-mus-ergo-01",
   "description": "USB • 1200 / 1600 / 2400 / 3200 DPI • Pour droitier",
   "priceHt": "25.900",
   "stock": 10,
   "image": "/uploads/c3ef7406-30a7-5973-b3e7-41057f462202.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "MUS-ERGO-01"
    },
    {
     "name": "EAN",
     "value": "8716309104890"
    }
   ],
   "cat": "souris-microphones"
  },
  {
   "sku": "MUS-ERGO-03",
   "name": "Souris optique filaire ergonomique 6 boutons, gris clair",
   "slug": "souris-optique-filaire-ergonomique-6-boutons-gris-clair-mus-ergo-03",
   "description": "USB • 800 / 1600 / 2400 / 3200 DPI",
   "priceHt": "39.900",
   "stock": 10,
   "image": "/uploads/22d309d4-86fd-5176-a041-4737aee864cb.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "MUS-ERGO-03"
    },
    {
     "name": "EAN",
     "value": "8716309109000"
    }
   ],
   "cat": "souris-microphones"
  },
  {
   "sku": "MUSW3-9B-01",
   "name": "Souris sans fil 2,4 GHz + Bluetooth",
   "slug": "souris-sans-fil-2-4-ghz-bluetooth-musw3-9b-01",
   "description": "Bluetooth 5.1 + USB • Capteur Pixart 3212 • 800 / 1600 / 2400 DPI • 9 boutons",
   "priceHt": "55.900",
   "stock": 24,
   "image": "/uploads/737677a9-0a95-50b4-8e5c-3b6d6503738d.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "MUSW3-9B-01"
    },
    {
     "name": "EAN",
     "value": "8716309136914"
    }
   ],
   "cat": "souris-microphones"
  },
  {
   "sku": "MUSW-ERGO-01",
   "name": "Souris optique sans fil ergonomique 6 boutons, noire",
   "slug": "souris-optique-sans-fil-ergonomique-6-boutons-noire-musw-ergo-01",
   "description": null,
   "priceHt": "31.500",
   "stock": 40,
   "image": "/uploads/ecc32791-47b4-5b33-a04c-e638c90ffe5e.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "MUSW-ERGO-01"
    },
    {
     "name": "EAN",
     "value": "8716309104876"
    }
   ],
   "cat": "souris-microphones"
  },
  {
   "sku": "CK-CAD-FL400-01",
   "name": "Bombe dépoussiérante à air sec, 400 ml",
   "slug": "bombe-depoussierante-a-air-sec-400-ml-ck-cad-fl400-01",
   "description": null,
   "priceHt": "8.900",
   "stock": 24,
   "image": "/uploads/54102a04-e2f1-5e88-9031-822b6ea0b0fb.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "CK-CAD-FL400-01"
    },
    {
     "name": "EAN",
     "value": "8716309100588"
    }
   ],
   "cat": "entretien"
  },
  {
   "sku": "CK-CAD-FL750-01",
   "name": "Spray air sec dépoussiérant, 750 ml",
   "slug": "spray-air-sec-depoussierant-750-ml-ck-cad-fl750-01",
   "description": null,
   "priceHt": "13.900",
   "stock": 60,
   "image": "/uploads/a1bd9f22-5ae3-5f81-af63-5a5e967dc38f.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "CK-CAD-FL750-01"
    },
    {
     "name": "EAN",
     "value": "8716309117173"
    }
   ],
   "cat": "entretien"
  },
  {
   "sku": "CK-LCD-07",
   "name": "Kit de nettoyage 6 en 1",
   "slug": "kit-de-nettoyage-6-en-1-ck-lcd-07",
   "description": null,
   "priceHt": "11.900",
   "stock": 48,
   "image": "/uploads/591b45a6-59fd-5c77-9565-700330dc03c6.jpg",
   "specs": [
    {
     "name": "Référence",
     "value": "CK-LCD-07"
    },
    {
     "name": "EAN",
     "value": "8716309129497"
    }
   ],
   "cat": "entretien"
  }
 ]
};

// prix TTC arrondi au millime : calcul en millimes entiers pour éviter les erreurs d'arrondi
function prixVente(ht) {
  const millimesHt = Math.round(Number(ht) * 1000);
  const millimes = Math.round(millimesHt * (1 + MARGE) * (1 + TVA));
  return NumberDecimal((millimes / 1000).toFixed(3));
}

print(`Import GEMBIRD : ${DATA.categories.length} catégories, ${DATA.products.length} produits — TVA ${TVA * 100} %, marge ${MARGE * 100} %${DRY_RUN ? " (simulation)" : ""}`);

if (DRY_RUN) {
  DATA.products.slice(0, 5).forEach(p => print(`  ${p.sku.padEnd(16)} ${p.priceHt} HT -> ${prixVente(p.priceHt)} TTC  ${p.name}`));
  print("  …  (DRY_RUN=1 : rien n'a été écrit)");
  quit(0);
}

const now = new Date();

// 1) Catégories
const cats = {};
let catsCreees = 0;
for (const c of DATA.categories) {
  const r = db.categories.updateOne(
    { slug: c.slug },
    { $setOnInsert: { name: c.name, slug: c.slug, createdAt: now } },
    { upsert: true });
  if (r.upsertedCount) catsCreees++;
  const doc = db.categories.findOne({ slug: c.slug });
  cats[c.slug] = { _id: doc._id, name: doc.name, slug: doc.slug };   // = Product.CategoryRef
}

// 2) Produits
const ops = DATA.products.map(p => ({
  updateOne: {
    filter: { sku: p.sku },
    update: {
      $set: {
        name: p.name,
        brand: "Gembird",
        description: p.description,
        price: prixVente(p.priceHt),
        stock: NumberInt(p.stock),
        category: cats[p.cat],
        images: [p.image],
        specs: p.specs,
        updatedAt: now,
      },
      $setOnInsert: {
        sku: p.sku,
        slug: p.slug,
        oldPrice: null,
        active: true,
        featured: false,
        soldCount: NumberInt(0),
        rating: { average: Double(0), count: NumberInt(0) },
        createdAt: now,
      },
    },
    upsert: true,
  },
}));
const res = db.products.bulkWrite(ops, { ordered: false });

print(`Catégories : ${catsCreees} créée(s), ${DATA.categories.length - catsCreees} déjà présente(s)`);
print(`Produits   : ${res.upsertedCount} créé(s), ${res.matchedCount} mis à jour`);
print(`Total en base : ${db.products.countDocuments({ brand: "Gembird" })} produits Gembird`);
